package com.easycash.data

import androidx.room.Room
import com.easycash.data.local.CategoryEntity
import com.easycash.data.local.EasyCashDatabase
import com.easycash.data.local.DefaultCategories
import com.easycash.domain.Category
import com.easycash.domain.CategoryType
import com.easycash.domain.WriteResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], manifest = Config.NONE)
class CategoryStorageTest {
    private val category = Category(0, "Ăn uống", CategoryType.EXPENSE, "food", "expense", false)

    @Test
    fun `default categories seed once and preserve user edits on reopen`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "default-categories-test.db"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, EasyCashDatabase::class.java, name)
            .addCallback(DefaultCategories).build()
        try {
            val first = open()
            try {
                val repository = RoomCategoryRepository(first)
                val expenses = repository.observeAll(CategoryType.EXPENSE).first()
                assertEquals(4, expenses.size)
                assertTrue(expenses.all { it.isSystem })
                assertEquals(2, repository.observeAll(CategoryType.INCOME).first().size)
                val food = expenses.single { it.name == "Ăn uống" }
                assertEquals(WriteResult.Success(Unit), repository.update(food.copy(name = "Bữa ăn", iconKey = "custom")))
            } finally {
                first.close()
            }
            val second = open()
            try {
                val expenses = RoomCategoryRepository(second).observeAll(CategoryType.EXPENSE).first()
                assertEquals(4, expenses.size)
                assertEquals("custom", expenses.single { it.name == "Bữa ăn" }.iconKey)
            } finally {
                second.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun `duplicate writes roll back and archive preserves stored row`() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), EasyCashDatabase::class.java).build()
        try {
            val repository = RoomCategoryRepository(db)
            val firstId = (repository.add(category) as WriteResult.Success).value
            val secondId = (repository.add(category.copy(name = "Đi lại")) as WriteResult.Success).value
            assertEquals(WriteResult.Failure.CONFLICT, repository.add(category))
            assertEquals(WriteResult.Failure.CONFLICT, repository.update(category.copy(id = secondId)))
            assertEquals("Đi lại", db.categoryDao().findById(secondId)?.name)
            assertEquals(WriteResult.Success(Unit), repository.archive(firstId))
            assertTrue(db.categoryDao().findById(firstId)!!.isArchived)
            assertEquals(listOf(secondId), repository.observeAll(CategoryType.EXPENSE).first().map { it.id })
            assertEquals(WriteResult.Failure.NOT_FOUND, repository.archive(Long.MAX_VALUE))
            val systemId = db.categoryDao().insert(CategoryEntity(
                name = "Hệ thống", type = "EXPENSE", iconKey = "food", colorKey = "expense",
                isSystem = true, createdAt = 1, updatedAt = 1,
            ))
            assertEquals(WriteResult.Failure.INVALID_INPUT, repository.archive(systemId))
        } finally {
            db.close()
        }
    }

    @Test
    fun `category survives closing and reopening database`() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val name = "category-persistence-test.db"
        context.deleteDatabase(name)
        try {
            val first = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).build()
            try {
                assertTrue(RoomCategoryRepository(first).add(category) is WriteResult.Success)
            } finally {
                first.close()
            }
            val reopened = Room.databaseBuilder(context, EasyCashDatabase::class.java, name).build()
            try {
                assertEquals("Ăn uống", RoomCategoryRepository(reopened).observeAll(CategoryType.EXPENSE).first().single().name)
            } finally {
                reopened.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
