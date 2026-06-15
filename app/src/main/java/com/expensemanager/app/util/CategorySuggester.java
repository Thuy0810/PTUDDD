package com.expensemanager.app.util;

import androidx.annotation.Nullable;

import com.expensemanager.app.data.model.Category;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Bộ phân loại danh mục thông minh chạy hoàn toàn trên máy (offline).
 *
 * <p>Cách hoạt động:
 * <ul>
 *   <li>Chuẩn hóa văn bản: bỏ dấu tiếng Việt, đổi "đ" → "d", về chữ thường.</li>
 *   <li>So khớp với bộ từ khóa mở rộng cho từng danh mục. Mỗi từ khóa khớp
 *       cộng điểm theo độ dài (cụm dài, cụ thể hơn ⇒ điểm cao hơn).</li>
 *   <li>Chọn danh mục có tổng điểm cao nhất.</li>
 * </ul>
 *
 * <p>Trả về mã danh mục trùng với id danh mục mặc định (xem {@code SeedData}):
 * {@code food, transport, shopping, bills, education, entertainment, health,
 * family, saving} (chi tiêu) và {@code salary, bonus, gift, sales} (thu nhập).
 */
public final class CategorySuggester {

    /** Kết quả phân loại. */
    public static final class Suggestion {
        /** Mã danh mục gợi ý (vd "food"); có thể null nếu không nhận diện được. */
        @Nullable public final String categoryId;
        /** Loại giao dịch: {@link Category#TYPE_INCOME} hoặc {@link Category#TYPE_EXPENSE}. */
        public final String type;
        /** Điểm tin cậy thô (tổng độ dài từ khóa khớp). 0 nghĩa là đoán mặc định. */
        public final int score;

        Suggestion(@Nullable String categoryId, String type, int score) {
            this.categoryId = categoryId;
            this.type = type;
            this.score = score;
        }

        public boolean isConfident() {
            return categoryId != null && score > 0;
        }
    }

    /** Danh mục thu nhập (để suy ra loại giao dịch). */
    private static final List<String> INCOME_CATEGORIES = new ArrayList<>();
    /** keyword (đã bỏ dấu) -> categoryId. Dùng LinkedHashMap để ổn định thứ tự. */
    private static final Map<String, String> KEYWORDS = new LinkedHashMap<>();

    private static final Pattern COMBINING = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    static {
        INCOME_CATEGORIES.add("salary");
        INCOME_CATEGORIES.add("bonus");
        INCOME_CATEGORIES.add("gift");
        INCOME_CATEGORIES.add("sales");

        // ===== CHI TIÊU =====
        // Ăn uống
        put("food",
                "an uong", "do an", "do uong", "an sang", "an trua", "an toi", "an vat",
                "com", "pho", "bun", "mi", "chao", "banh mi", "banh", "xoi", "lau", "nuong",
                "buffet", "nha hang", "quan an", "quan", "tra sua", "ca phe", "cafe", "coffee",
                "tra", "nuoc ngot", "sinh to", "kem", "bia", "ruou", "snack", "trai cay",
                "hoa qua", "sua", "an", "uong");
        // Đi lại
        put("transport",
                "xang", "do xang", "grab", "gojek", "be", "taxi", "xe om", "xe buyt", "bus",
                "ve xe", "ve tau", "ve may bay", "may bay", "tau", "gui xe", "do xe", "giu xe",
                "parking", "sua xe", "rua xe", "bao duong xe", "di lai", "ve", "xe");
        // Mua sắm
        put("shopping",
                "mua sam", "shopee", "lazada", "tiki", "sendo", "thoi trang", "quan ao",
                "ao", "quan", "vay", "giay", "dep", "tui", "vi", "my pham", "son", "nuoc hoa",
                "phu kien", "dien thoai", "laptop", "do dung", "mua");
        // Hóa đơn
        put("bills",
                "hoa don", "tien dien", "tien nuoc", "dien", "nuoc", "tien mang", "mang",
                "internet", "wifi", "cuoc dien thoai", "cuoc", "tien nha", "thue nha", "thue phong",
                "gas", "ga", "truyen hinh", "netflix", "spotify", "youtube premium", "dich vu",
                "hoa don dien", "hoa don nuoc");
        // Học tập
        put("education",
                "hoc phi", "tien hoc", "khoa hoc", "lop hoc", "gia su", "sach", "vo", "but",
                "tai lieu", "dao tao", "le phi thi", "the hoc", "hoc", "course");
        // Giải trí
        put("entertainment",
                "giai tri", "xem phim", "phim", "rap phim", "game", "choi game", "du lich",
                "di choi", "karaoke", "concert", "ca nhac", "bar", "club", "bowling", "ve concert",
                "ve phim", "choi");
        // Sức khỏe
        put("health",
                "suc khoe", "thuoc", "kham benh", "kham", "benh vien", "bac si", "nha khoa",
                "rang", "vien phi", "bao hiem y te", "gym", "phong gym", "tap gym", "yoga",
                "spa", "massage", "phong kham");
        // Gia đình
        put("family",
                "gia dinh", "tien hoc con", "con", "bo me", "cha me", "ong ba", "bieu",
                "qua cho", "hieu hi", "dam cuoi", "dam ma", "mung cuoi", "tien mung");
        // Tiết kiệm / đầu tư
        put("saving",
                "tiet kiem", "gui tiet kiem", "dau tu", "co phieu", "chung khoan", "vang",
                "quy", "gop von");

        // ===== THU NHẬP =====
        put("salary", "tien luong", "luong thang", "luong", "salary");
        put("bonus", "thuong tet", "thuong le", "tien thuong", "thuong", "bonus");
        put("gift", "duoc cho", "tien cho", "qua tang", "mung tuoi", "li xi", "lixi", "duoc bieu");
        put("sales", "ban hang", "doanh thu", "tien ban", "lam them", "freelance",
                "lam ngoai", "hoa hong", "ban");
    }

    private static void put(String categoryId, String... keywords) {
        for (String k : keywords) {
            KEYWORDS.put(normalize(k), categoryId);
        }
    }

    private CategorySuggester() {}

    /**
     * Bỏ dấu tiếng Việt, đổi đ→d, về chữ thường, gộp khoảng trắng.
     */
    public static String normalize(String input) {
        if (input == null) return "";
        String lower = input.toLowerCase(Locale.ROOT)
                .replace('đ', 'd');
        String decomposed = Normalizer.normalize(lower, Normalizer.Form.NFD);
        String noMarks = COMBINING.matcher(decomposed).replaceAll("");
        return noMarks.replaceAll("\\s+", " ").trim();
    }

    /**
     * Phân loại văn bản và trả về kết quả chi tiết (mã danh mục + loại + điểm).
     * Không bao giờ trả về null; nếu không nhận diện được sẽ trả về
     * {@code categoryId == null}, {@code type == expense}.
     */
    public static Suggestion classify(String text) {
        String norm = normalize(text);
        if (norm.isEmpty()) {
            return new Suggestion(null, Category.TYPE_EXPENSE, 0);
        }

        Map<String, Integer> scores = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : KEYWORDS.entrySet()) {
            String kw = e.getKey();
            if (containsWord(norm, kw)) {
                String catId = e.getValue();
                Integer cur = scores.get(catId);
                scores.put(catId, (cur == null ? 0 : cur) + kw.length());
            }
        }

        String bestId = null;
        int bestScore = 0;
        for (Map.Entry<String, Integer> e : scores.entrySet()) {
            if (e.getValue() > bestScore) {
                bestScore = e.getValue();
                bestId = e.getKey();
            }
        }

        String type = (bestId != null && INCOME_CATEGORIES.contains(bestId))
                ? Category.TYPE_INCOME : Category.TYPE_EXPENSE;
        return new Suggestion(bestId, type, bestScore);
    }

    /**
     * So khớp từ khóa theo ranh giới từ để tránh khớp nhầm
     * (vd "an" không khớp trong "ban"). Cụm nhiều từ vẫn khớp bình thường.
     */
    private static boolean containsWord(String haystack, String keyword) {
        int idx = 0;
        while ((idx = haystack.indexOf(keyword, idx)) >= 0) {
            boolean leftOk = idx == 0 || haystack.charAt(idx - 1) == ' ';
            int end = idx + keyword.length();
            boolean rightOk = end == haystack.length() || haystack.charAt(end) == ' ';
            if (leftOk && rightOk) return true;
            idx = end;
        }
        return false;
    }

    /**
     * Tương thích ngược: chỉ trả về mã danh mục gợi ý (hoặc null).
     */
    @Nullable
    public static String suggestCategoryId(String note) {
        Suggestion s = classify(note);
        return s.categoryId;
    }
}
