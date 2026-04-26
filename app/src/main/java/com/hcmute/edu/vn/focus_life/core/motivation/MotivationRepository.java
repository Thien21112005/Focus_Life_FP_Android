package com.hcmute.edu.vn.focus_life.core.motivation;

import java.util.Calendar;

public final class MotivationRepository {
    private static final MotivationQuote[] HEALTH_QUOTES = new MotivationQuote[] {
            new MotivationQuote("Một cơ thể khỏe hơn bắt đầu từ một lựa chọn nhỏ hôm nay: uống nước, đứng dậy và làm tiếp thật bình tĩnh.", "FocusLife"),
            new MotivationQuote("Bạn không cần hoàn hảo. Chỉ cần đều đặn hơn hôm qua một chút là đã đủ để tiến bộ.", "FocusLife"),
            new MotivationQuote("Sức khỏe là món quà bạn tự tặng mình bằng những hành động nhỏ lặp lại mỗi ngày.", "FocusLife"),
            new MotivationQuote("Uống một ly nước, hít thở sâu và chọn một việc quan trọng. Ngày hôm nay sẽ nhẹ hơn rất nhiều.", "FocusLife"),
            new MotivationQuote("Một phiên tập trung ngắn cũng có giá trị nếu bạn thật sự có mặt trong từng phút.", "FocusLife"),
            new MotivationQuote("Đừng đợi có động lực mới bắt đầu. Hãy bắt đầu, rồi động lực sẽ đến sau.", "FocusLife"),
            new MotivationQuote("Cơ thể bạn đang đồng hành cùng bạn mỗi ngày. Hãy chăm sóc nó bằng nước, giấc ngủ và vận động.", "FocusLife"),
            new MotivationQuote("Đi thêm vài bước hôm nay là cách đơn giản để nhắc bản thân rằng bạn vẫn đang cố gắng.", "FocusLife"),
            new MotivationQuote("Một ngày tốt không cần quá nhiều việc. Chỉ cần khỏe hơn, tỉnh táo hơn và bình yên hơn.", "FocusLife"),
            new MotivationQuote("Khi thấy mệt, hãy quay về điều cơ bản: uống nước, thở chậm và làm một việc nhỏ.", "FocusLife"),
            new MotivationQuote("Bạn đang xây một phiên bản tốt hơn bằng từng bữa ăn, từng bước chân và từng phút tập trung.", "FocusLife"),
            new MotivationQuote("Sự bền bỉ không ồn ào. Nó nằm trong việc bạn vẫn chọn chăm sóc bản thân hôm nay.", "FocusLife"),
            new MotivationQuote("Tập trung là chăm sóc tâm trí. Vận động là chăm sóc cơ thể. Cả hai đều là yêu bản thân.", "FocusLife"),
            new MotivationQuote("Hôm nay chậm cũng được, miễn là bạn không bỏ rơi sức khỏe của mình.", "FocusLife"),
            new MotivationQuote("Một ly nước đúng lúc có thể là khởi đầu cho một nhịp sống cân bằng hơn.", "FocusLife"),
            new MotivationQuote("Bạn xứng đáng có một ngày đủ năng lượng, đủ tỉnh táo và đủ dịu dàng với chính mình.", "FocusLife"),
            new MotivationQuote("Đừng xem nhẹ những thói quen nhỏ. Chúng âm thầm tạo ra thay đổi lớn.", "FocusLife"),
            new MotivationQuote("Hãy làm điều tốt cho cơ thể trước khi cơ thể phải nhắc bạn bằng sự mệt mỏi.", "FocusLife"),
            new MotivationQuote("Nếu hôm nay chưa tốt, vẫn còn một lựa chọn nhỏ để bắt đầu lại: uống nước và làm tiếp 5 phút.", "FocusLife"),
            new MotivationQuote("Năng lượng không tự nhiên xuất hiện. Nó đến từ cách bạn ăn, ngủ, vận động và nghỉ ngơi.", "FocusLife"),
            new MotivationQuote("Mỗi lần bạn chọn lành mạnh hơn, bạn đang bỏ phiếu cho tương lai của mình.", "FocusLife"),
            new MotivationQuote("Không cần chạy thật xa. Chỉ cần bước ra khỏi sự trì hoãn là bạn đã thắng một đoạn đường.", "FocusLife"),
            new MotivationQuote("Hãy để hôm nay là một ngày bạn đối xử tử tế hơn với cơ thể của mình.", "FocusLife"),
            new MotivationQuote("Một phút tập trung thật sự tốt hơn mười phút vừa làm vừa kiệt sức.", "FocusLife"),
            new MotivationQuote("Cứ nhẹ nhàng mà đều đặn. Cơ thể sẽ cảm ơn bạn vì những điều nhỏ bạn làm mỗi ngày.", "FocusLife"),
            new MotivationQuote("Sức khỏe không phải đích đến xa xôi. Nó là cách bạn sống trong từng giờ hôm nay.", "FocusLife"),
            new MotivationQuote("Bạn không cần thay đổi cả cuộc đời trong một ngày. Hãy thay đổi một lựa chọn trước.", "FocusLife"),
            new MotivationQuote("Hôm nay hãy chọn một điều đơn giản: uống đủ nước, ăn vừa đủ hoặc đi bộ thêm vài phút.", "FocusLife"),
            new MotivationQuote("Khi tâm trí rối, hãy đưa cơ thể về nhịp ổn định: thở chậm, uống nước và bắt đầu lại.", "FocusLife"),
            new MotivationQuote("Một thói quen tốt không cần hoàn hảo, chỉ cần được lặp lại đủ lâu.", "FocusLife"),
            new MotivationQuote("Bạn đang làm tốt hơn bạn nghĩ. Tiếp tục thêm một bước nhỏ nữa thôi.", "FocusLife"),
            new MotivationQuote("Ngày hôm nay không cần vội. Hãy khỏe trước, rồi hiệu quả sẽ đến sau.", "FocusLife"),
            new MotivationQuote("Đặt điện thoại xuống vài phút, đứng dậy, uống nước và cho mắt được nghỉ ngơi.", "FocusLife"),
            new MotivationQuote("Từng bước chân, từng ly nước và từng phiên Focus đều là bằng chứng bạn đang chăm sóc mình.", "FocusLife"),
            new MotivationQuote("Sự thay đổi bền vững thường bắt đầu bằng một việc nhỏ đến mức bạn có thể làm ngay bây giờ.", "FocusLife"),
            new MotivationQuote("Đừng chờ đến khi kiệt sức mới nghỉ. Nghỉ đúng lúc cũng là một kỹ năng chăm sóc sức khỏe.", "FocusLife"),
            new MotivationQuote("Hãy chọn tiến bộ thay vì áp lực. Cơ thể và tâm trí của bạn cần sự kiên nhẫn.", "FocusLife"),
            new MotivationQuote("Một ngày lành mạnh là ngày bạn nhớ chăm sóc cả công việc lẫn chính mình.", "FocusLife"),
            new MotivationQuote("Mục tiêu lớn không đáng sợ nếu hôm nay bạn bắt đầu bằng một hành động nhỏ.", "FocusLife"),
            new MotivationQuote("Bạn của ngày mai sẽ biết ơn vì hôm nay bạn đã không bỏ cuộc.", "FocusLife")
    };

    private static final String[] DAILY_TIPS = new String[] {
            "Uống một ly nước trước khi bắt đầu học/làm việc, rồi đứng dậy vận động nhẹ sau mỗi phiên Focus.",
            "Hôm nay hãy chọn một mục tiêu nhỏ: đi bộ thêm vài phút hoặc hoàn thành một phiên Focus ngắn.",
            "Ăn chậm hơn một chút và ghi lại bữa ăn để hiểu cơ thể mình đang cần gì.",
            "Khi mệt, đừng cố quá lâu. Nghỉ 3 phút, uống nước và quay lại với nhịp nhẹ hơn.",
            "Nếu ngồi lâu, hãy đứng dậy vươn vai. Một chuyển động nhỏ cũng giúp bạn tỉnh táo hơn.",
            "Buổi tối hãy tổng kết nhẹ: hôm nay bạn đã chăm sóc bản thân bằng điều gì?",
            "Hãy đặt một ly nước gần nơi học/làm việc để nhắc mình uống đủ hơn.",
            "Đừng bỏ bữa quá lâu. Năng lượng ổn định giúp bạn tập trung tốt hơn.",
            "Đi bộ 5 phút sau khi ngồi lâu cũng là một cách reset cơ thể.",
            "Trước khi ngủ, hãy để ngày mai bắt đầu dễ hơn bằng một kế hoạch thật nhỏ."
    };

    private MotivationRepository() {}

    public static int getQuoteCount() {
        return HEALTH_QUOTES.length;
    }

    public static MotivationQuote getQuoteByIndex(int index) {
        int safeIndex = Math.abs(index) % HEALTH_QUOTES.length;
        return HEALTH_QUOTES[safeIndex];
    }

    public static MotivationQuote getQuoteForNow() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int timeSlot = hour < 11 ? 0 : hour < 14 ? 7 : hour < 18 ? 14 : 21;
        int index = Math.abs(day + timeSlot) % HEALTH_QUOTES.length;
        return HEALTH_QUOTES[index];
    }

    public static MotivationQuote getQuoteForSlot(int slot) {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_YEAR);
        int index = Math.abs(day + slot) % HEALTH_QUOTES.length;
        return HEALTH_QUOTES[index];
    }

    public static String getDailyTip() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return DAILY_TIPS[Math.abs(day) % DAILY_TIPS.length];
    }

    public static String getNotificationTitle(String displayName) {
        String name = displayName == null || displayName.trim().isEmpty() ? "bạn" : displayName.trim();
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 11) return name + " ơi, bắt đầu ngày khỏe hơn";
        if (hour < 14) return name + " ơi, giữ năng lượng giữa ngày";
        if (hour < 18) return name + " ơi, nhắc nhẹ cho buổi chiều";
        return name + " ơi, khép ngày thật nhẹ nhàng";
    }
}
