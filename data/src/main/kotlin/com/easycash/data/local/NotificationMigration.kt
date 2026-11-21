package com.easycash.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS notification_sources (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, package_name TEXT NOT NULL, display_name TEXT NOT NULL, parser_id TEXT NOT NULL, is_enabled INTEGER NOT NULL, linked_account_id INTEGER, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, FOREIGN KEY(linked_account_id) REFERENCES accounts(id) ON UPDATE NO ACTION ON DELETE SET NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_sources_package_name ON notification_sources(package_name)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_sources_linked_account_id ON notification_sources(linked_account_id)")
        db.execSQL("CREATE TABLE IF NOT EXISTS notification_events (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, source_id INTEGER NOT NULL, posted_at INTEGER NOT NULL, fingerprint TEXT NOT NULL, classification TEXT NOT NULL, parse_status TEXT NOT NULL, parser_version INTEGER NOT NULL, expires_at INTEGER NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(source_id) REFERENCES notification_sources(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_events_source_id_fingerprint ON notification_events(source_id, fingerprint)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_posted_at ON notification_events(posted_at)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_events_expires_at ON notification_events(expires_at)")
    }
}
