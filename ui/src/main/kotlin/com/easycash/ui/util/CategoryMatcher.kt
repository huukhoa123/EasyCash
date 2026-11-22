package com.easycash.ui.util

import com.easycash.domain.Category

object CategoryMatcher {
    private val keywordMap = mapOf(
        "Ăn uống" to listOf(
            "ăn", "uống", "cơm", "phở", "bún", "mì", "cà phê", "cafe", "coffee", "trà", "milk tea",
            "bánh", "lẩu", "nướng", "thịt", "cá", "quán", "nhà hàng", "trưa", "sáng", "tối",
            "highlands", "starbucks", "phúc long", "katinat", "the coffee house", "pizza", "kfc", "lotteria",
        ),
        "Di chuyển" to listOf(
            "xăng", "xe", "grab", "be", "gojek", "xanh sm", "taxi", "gửi xe", "vé xe", "vé tàu",
            "vé máy bay", "vietjet", "vietnam airlines", "bảo dưỡng", "rửa xe", "nhớt", "cầu đường", "bot",
        ),
        "Mua sắm" to listOf(
            "shopee", "lazada", "tiki", "tiktok shop", "mua", "quần", "áo", "giày", "dép", "siêu thị",
            "chợ", "tạp hoá", "đồ dùng", "coopmart", "winmart", "bách hoá xanh", "uniqlo", "zara",
        ),
        "Hoá đơn" to listOf(
            "điện", "nước", "internet", "wifi", "mạng", "tiền nhà", "phòng", "chung cư", "phí quản lý",
            "hoá đơn", "truyền hình", "fpt", "viettel", "vnpt", "evn",
        ),
        "Sức khoẻ" to listOf(
            "thuốc", "khám", "bệnh viện", "bác sĩ", "nha khoa", "răng", "mắt", "gym", "thể thao",
            "pharmacity", "long châu", "an khang",
        ),
        "Giải trí" to listOf(
            "phim", "cinema", "cgv", "lotte cinema", "bhd", "game", "steam", "nintendo", "playstation",
            "du lịch", "hotel", "resort", "agoda", "booking", "hát", "karaoke", "netflix", "spotify",
        ),
        "Lương" to listOf(
            "lương", "salary", "thưởng", "bonus", "lãi", "thu nhập", "tiền về", "hoa hồng",
        ),
    )

    private fun matchesKeyword(text: String, keyword: String): Boolean {
        val pattern = Regex("(?:^|[^\\p{L}\\p{N}])${Regex.escape(keyword)}(?:$|[^\\p{L}\\p{N}])", RegexOption.IGNORE_CASE)
        return pattern.containsMatchIn(text)
    }

    fun suggestCategory(note: String, categories: List<Category>): Category? {
        val trimmed = note.trim()
        if (trimmed.isBlank() || categories.isEmpty()) return null

        // 1. Kiểm tra trực tiếp tên danh mục xuất hiện trong ghi chú
        for (cat in categories) {
            val catName = cat.name.trim()
            if (catName.length >= 2 && matchesKeyword(trimmed, catName)) {
                return cat
            }
        }

        // 2. Tra cứu từ khoá ngữ nghĩa
        for ((catName, keywords) in keywordMap) {
            val targetCategory = categories.find { it.name.equals(catName, ignoreCase = true) }
            if (targetCategory != null) {
                if (keywords.any { keyword -> matchesKeyword(trimmed, keyword) }) {
                    return targetCategory
                }
            }
        }

        return null
    }
}
