package com.zentrade.ai.ui.office;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Kantor 2D animasi untuk agent Zentrade AI.
 * Semua koordinat memakai "dunia virtual" 360 x 240 lalu di-scale ke ukuran View.
 *
 * Perilaku:
 *  - Mode IDLE  : agent bekerja di meja, sesekali pergi buat kopi atau ngobrol.
 *  - Mode SETUP : (setTradeSetupActive(true)) semua agent kembali ke meja dan fokus bekerja.
 *
 * Jumlah agent bisa diubah lewat addAgent(...) / removeAgent(...).
 */
public class OfficeView extends View {

    // ---------- Dunia virtual ----------
    private static final float W = 360f;
    private static final float H = 240f;
    private static final float WALL_H = 44f;
    private static final float DESK_Y = 62f;
    private static final float DESK_H = 24f;
    private static final float SEAT_Y = 106f;
    private static final float LANE_Y = 132f;
    private static final float COFFEE_X = 322f;
    private static final float COFFEE_Y = 106f;
    private static final float SPEED = 42f;
    private static final float BREW_TIME = 3.2f;

    // ---------- Warna ----------
    private static final int C_WALL = Color.parseColor("#1B1B1F");
    private static final int C_FLOOR_A = Color.parseColor("#15161A");
    private static final int C_FLOOR_B = Color.parseColor("#121316");
    private static final int C_GOLD = Color.parseColor("#F2CA50");
    private static final int C_TEXT = Color.parseColor("#E3E2E6");
    private static final int C_TEXT_DIM = Color.parseColor("#D0C5AF");
    private static final int C_DESK = Color.parseColor("#2E2A24");
    private static final int C_DESK_EDGE = Color.parseColor("#4B4333");
    private static final int C_SKIN = Color.parseColor("#F1C9A5");

    private enum State { WORK, WALK, BREW, CHAT_WAIT, CHAT }
    private enum Next { WORK, COFFEE, CHAT }

    /** Data agent. */
    private static class Agent {
        String name;
        String role;       // label singkat, mis. "TA"
        int color;
        String[] lines;    // kalimat obrolan
        float x, y;
        State state = State.WORK;
        Next next = Next.WORK;
        final List<float[]> path = new ArrayList<>();
        float workTimer;
        float stateTime;
        float walkPhase;
        boolean facingUp = true;
        int faceDir = 1;
        String bubble;
        float bubbleTimer;
        float cupTimer;
        Agent partner;
        boolean initiator;
        float chatTime;
        float chatDur;
        int lastSlot = -1;
        // info tambahan untuk chip + panel detail (sementara data contoh)
        String task = "";
        String workStatus = "Working";
        String workLine;
        int confidence = 50;
        String lastDecision = "-";
        float statusTimer = 2f;
    }

    /** Ringkasan agent untuk UI di luar kanvas (chip, panel detail). */
    public static class AgentInfo {
        public String name;
        public String role;
        public String status;
        public String task;
        public String lastDecision;
        public int color;
        public int confidence;
        public boolean speaking;
    }

    private final List<Agent> agents = new ArrayList<>();
    private final Random rnd = new Random();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final Path tmpPath = new Path();

    private float[] deskX = new float[0];
    private float deskW = 54f;
    private Agent coffeeUser;
    private boolean setupActive = false;
    private float time = 0f;
    private long lastNanos = 0L;
    private boolean running = false;

    private final Runnable loop = new Runnable() {
        @Override
        public void run() {
            if (!running) return;
            long now = System.nanoTime();
            float dt = lastNanos == 0L ? 0.016f : (now - lastNanos) / 1e9f;
            lastNanos = now;
            if (dt > 0.05f) dt = 0.05f;
            update(dt);
            invalidate();
            postOnAnimation(this);
        }
    };

    public OfficeView(Context c) { super(c); init(); }
    public OfficeView(Context c, AttributeSet a) { super(c, a); init(); }
    public OfficeView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        // 4 agent default
        addAgent("Atlas", "TA", Color.parseColor("#E9C176"), new String[]{
                "EURUSD tahan support H1", "RSI mulai divergen", "Ada order block di M15", "Trend H4 masih bullish"},
                "Analisis teknikal: struktur harga, support/resistance, dan indikator.",
                "Analyzing", "Menganalisa EURUSD H1", 78, "Bias bullish EURUSD (H4)");
        addAgent("Nova", "NEWS", Color.parseColor("#7FB2FF"), new String[]{
                "NFP rilis jam 19:30", "Berita ECB agak hawkish", "Hati-hati spread melebar", "CPI USD naik tipis"},
                "Memantau berita ekonomi dan kalender high-impact.",
                "Monitoring", "Memantau berita USD", 64, "Impact tinggi: NFP 19:30 WIB");
        addAgent("Guard", "RISK", Color.parseColor("#FFB4AB"), new String[]{
                "Drawdown aman 1.2%", "Margin level sehat", "Risk per trade 1%", "Exposure masih rendah"},
                "Mengawasi drawdown, margin, dan batas risiko harian.",
                "Guarding", "Drawdown 1.2% - aman", 92, "Drawdown 1.2% dari batas 5%");
        addAgent("Exec", "EXEC", Color.parseColor("#58E7AA"), new String[]{
                "Order siap dieksekusi", "Latency 14 ms, aman", "Broker sinkron", "Slippage minimal"},
                "Mengeksekusi order setelah semua agent menyetujui.",
                "Ready", "Menunggu sinyal", 85, "Belum ada order hari ini");
    }

    // ---------- API publik ----------

    /** Tambah agent baru (meja otomatis diatur ulang). */
    public void addAgent(String name, String roleLabel, int color, String[] chatLines) {
        addAgent(name, roleLabel, color, chatLines, "", "Working", null, 50, "-");
    }

    /** Tambah agent lengkap dengan info untuk chip dan panel detail. */
    public void addAgent(String name, String roleLabel, int color, String[] chatLines,
                         String task, String workStatus, String workLine,
                         int confidence, String lastDecision) {
        Agent a = new Agent();
        a.task = task;
        a.workStatus = workStatus;
        a.workLine = workLine;
        a.confidence = confidence;
        a.lastDecision = lastDecision;
        a.statusTimer = 1f + rnd.nextFloat() * 3f;
        a.name = name;
        a.role = roleLabel;
        a.color = color;
        a.lines = chatLines;
        a.workTimer = 2f + rnd.nextFloat() * 5f;
        agents.add(a);
        layoutDesks();
        a.x = deskX[agents.size() - 1] + deskW / 2f;
        a.y = SEAT_Y;
    }

    /** Hapus agent berdasarkan index. */
    public void removeAgent(int index) {
        if (index < 0 || index >= agents.size()) return;
        Agent a = agents.get(index);
        releaseSocial(a);
        if (coffeeUser == a) coffeeUser = null;
        agents.remove(index);
        layoutDesks();
        for (Agent o : agents) {
            if (o.state == State.WORK) {
                int i = agents.indexOf(o);
                o.x = deskX[i] + deskW / 2f;
                o.y = SEAT_Y;
            } else {
                sendToDesk(o);
            }
        }
    }

    public int getAgentCount() { return agents.size(); }

    /** Info agent untuk chip / panel detail. Aman dipanggil dari main thread. */
    public AgentInfo getAgentInfo(int i) {
        if (i < 0 || i >= agents.size()) return null;
        Agent a = agents.get(i);
        AgentInfo in = new AgentInfo();
        in.name = a.name;
        in.role = a.role;
        in.color = a.color;
        in.task = a.task;
        in.confidence = a.confidence;
        in.lastDecision = a.lastDecision;
        in.speaking = a.bubble != null;
        switch (a.state) {
            case WORK: in.status = setupActive ? a.workStatus : "Standby"; break;
            case WALK: in.status = "Walking"; break;
            case BREW: in.status = "Coffee"; break;
            default: in.status = "Chatting"; break;
        }
        return in;
    }

    /** true = ada setup trading, semua agent fokus di meja. */
    public void setTradeSetupActive(boolean active) {
        if (setupActive == active) return;
        setupActive = active;
        if (active) {
            for (Agent a : agents) {
                if (a.state != State.WORK) {
                    releaseSocial(a);
                    if (coffeeUser == a) coffeeUser = null;
                    sendToDesk(a);
                }
                a.workTimer = 3f;
                a.statusTimer = 3.5f + rnd.nextFloat() * 3f;
            }
            Agent exec = agents.isEmpty() ? null : agents.get(agents.size() - 1);
            if (exec != null) say(exec, "Setup masuk!", 3f);
            if (agents.size() > 0) say(agents.get(0), "Analisa dimulai", 3f);
        } else {
            for (Agent a : agents) a.workTimer = 2f + rnd.nextFloat() * 5f;
        }
    }

    public boolean isTradeSetupActive() { return setupActive; }

    // ---------- Lifecycle ----------

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        running = true;
        lastNanos = 0L;
        postOnAnimation(loop);
    }

    @Override
    protected void onDetachedFromWindow() {
        running = false;
        removeCallbacks(loop);
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        if (w <= 0) w = 360;
        setMeasuredDimension(w, Math.round(w * H / W));
    }

    // ---------- Layout meja ----------

    private void layoutDesks() {
        int n = Math.max(1, agents.size());
        deskW = Math.min(54f, (252f - (n - 1) * 8f) / n);
        deskX = new float[n];
        for (int i = 0; i < n; i++) deskX[i] = 16f + i * (deskW + 8f);
    }

    private float seatX(Agent a) {
        int i = agents.indexOf(a);
        if (i < 0) i = 0;
        return deskX[Math.min(i, deskX.length - 1)] + deskW / 2f;
    }

    // ---------- Logika ----------

    private void update(float dt) {
        time += dt;
        for (Agent a : new ArrayList<>(agents)) updateAgent(a, dt);
    }

    private void updateAgent(Agent a, float dt) {
        if (a.bubbleTimer > 0f) {
            a.bubbleTimer -= dt;
            if (a.bubbleTimer <= 0f) a.bubble = null;
        }
        if (a.cupTimer > 0f) a.cupTimer -= dt;

        switch (a.state) {
            case WORK:
                a.facingUp = true;
                a.workTimer -= dt;
                if (setupActive && a.workLine != null) {
                    a.statusTimer -= dt;
                    if (a.statusTimer <= 0f && a.bubble == null) {
                        say(a, a.workLine, 2.8f);
                        a.statusTimer = 7f + rnd.nextFloat() * 4f;
                    }
                }
                if (a.workTimer <= 0f) {
                    if (setupActive) a.workTimer = 3f; else decideIdle(a);
                }
                break;

            case WALK:
                a.facingUp = false;
                moveAlong(a, dt);
                if (a.path.isEmpty()) arrive(a);
                break;

            case BREW:
                a.facingUp = true;
                a.stateTime += dt;
                if (a.stateTime >= BREW_TIME) {
                    if (coffeeUser == a) coffeeUser = null;
                    a.cupTimer = 16f;
                    say(a, "Mantap!", 1.6f);
                    sendToDesk(a);
                }
                break;

            case CHAT_WAIT:
                a.facingUp = false;
                if (a.partner == null) {
                    sendToDesk(a);
                } else if (a.partner.state == State.CHAT_WAIT) {
                    startChat(a, a.partner);
                }
                break;

            case CHAT:
                a.facingUp = false;
                a.chatTime += dt;
                int slot = ((int) (a.chatTime / 2.4f)) % 2;
                int mySlot = a.initiator ? 0 : 1;
                if (slot == mySlot && slot != a.lastSlot) {
                    a.lastSlot = slot;
                    say(a, a.lines[rnd.nextInt(a.lines.length)], 2.2f);
                } else if (slot != mySlot) {
                    a.lastSlot = slot;
                }
                if (a.initiator && a.chatTime >= a.chatDur) {
                    Agent b = a.partner;
                    releaseSocial(a);
                    sendToDesk(a);
                    if (b != null) sendToDesk(b);
                }
                break;
        }
    }

    private void decideIdle(Agent a) {
        float roll = rnd.nextFloat();
        if (roll < 0.40f && coffeeUser == null && a.cupTimer <= 0f) {
            coffeeUser = a;
            say(a, "Ngopi dulu...", 2f);
            goTo(a, COFFEE_X, COFFEE_Y, Next.COFFEE);
            return;
        }
        if (roll < 0.85f) {
            Agent b = pickChatPartner(a);
            if (b != null) {
                startChatWalk(a, b);
                return;
            }
        }
        a.workTimer = 3f + rnd.nextFloat() * 5f;
    }

    private Agent pickChatPartner(Agent a) {
        List<Agent> free = new ArrayList<>();
        for (Agent o : agents) {
            if (o != a && o.state == State.WORK && o.partner == null) free.add(o);
        }
        if (free.isEmpty()) return null;
        return free.get(rnd.nextInt(free.size()));
    }

    private void startChatWalk(Agent a, Agent b) {
        float mx = 130f + rnd.nextFloat() * 90f;
        float my = 170f + rnd.nextFloat() * 36f;
        a.partner = b;
        b.partner = a;
        a.initiator = true;
        b.initiator = false;
        say(a, "Eh, sini bentar", 1.8f);
        goTo(a, mx - 14f, my, Next.CHAT);
        goTo(b, mx + 14f, my, Next.CHAT);
    }

    private void startChat(Agent a, Agent b) {
        float dur = 6f + rnd.nextFloat() * 4f;
        a.state = State.CHAT;
        b.state = State.CHAT;
        a.chatTime = 0f;
        b.chatTime = 0f;
        a.chatDur = dur;
        b.chatDur = dur;
        a.lastSlot = -1;
        b.lastSlot = -1;
        a.faceDir = a.x <= b.x ? 1 : -1;
        b.faceDir = -a.faceDir;
    }

    private void releaseSocial(Agent a) {
        Agent p2 = a.partner;
        a.partner = null;
        a.initiator = false;
        if (p2 != null && p2.partner == a) {
            p2.partner = null;
            p2.initiator = false;
            if (p2.state == State.CHAT || p2.state == State.CHAT_WAIT
                    || (p2.state == State.WALK && p2.next == Next.CHAT)) {
                sendToDesk(p2);
            }
        }
    }

    private void sendToDesk(Agent a) {
        a.workTimer = 4f + rnd.nextFloat() * 6f;
        goTo(a, seatX(a), SEAT_Y, Next.WORK);
    }

    private void goTo(Agent a, float dx, float dy, Next next) {
        a.path.clear();
        if (a.y < LANE_Y - 4f) a.path.add(new float[]{a.x, LANE_Y});
        if (dy < LANE_Y - 4f) a.path.add(new float[]{dx, LANE_Y});
        a.path.add(new float[]{dx, dy});
        a.next = next;
        a.state = State.WALK;
    }

    private void arrive(Agent a) {
        switch (a.next) {
            case WORK:
                a.state = State.WORK;
                break;
            case COFFEE:
                a.state = State.BREW;
                a.stateTime = 0f;
                break;
            case CHAT:
                a.state = State.CHAT_WAIT;
                break;
        }
    }

    private void moveAlong(Agent a, float dt) {
        if (a.path.isEmpty()) return;
        float[] t = a.path.get(0);
        float dx = t[0] - a.x;
        float dy = t[1] - a.y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        float step = SPEED * dt;
        a.walkPhase += dt * 9f;
        if (Math.abs(dx) > 0.5f) a.faceDir = dx > 0 ? 1 : -1;
        if (step >= dist) {
            a.x = t[0];
            a.y = t[1];
            a.path.remove(0);
        } else {
            a.x += dx / dist * step;
            a.y += dy / dist * step;
        }
    }

    private void say(Agent a, String text, float seconds) {
        a.bubble = text;
        a.bubbleTimer = seconds;
    }

    // ---------- Gambar ----------

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float s = getWidth() / W;
        c.save();
        c.scale(s, s);

        drawRoom(c);
        drawCoffeeStation(c);
        for (int i = 0; i < deskX.length; i++) drawDesk(c, i);
        drawDecor(c);

        List<Agent> sorted = new ArrayList<>(agents);
        Collections.sort(sorted, new Comparator<Agent>() {
            @Override
            public int compare(Agent o1, Agent o2) {
                return Float.compare(o1.y, o2.y);
            }
        });
        for (Agent a : sorted) drawAgent(c, a);
        for (Agent a : sorted) drawBubble(c, a);

        drawStatusChip(c);
        c.restore();
    }

    private void fillRound(Canvas c, float l, float t, float rr, float b, float rad, int color) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        r.set(l, t, rr, b);
        c.drawRoundRect(r, rad, rad, p);
    }

    private void drawRoom(Canvas c) {
        // lantai
        for (int ty = 0; ty < 10; ty++) {
            for (int tx = 0; tx < 18; tx++) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(((tx + ty) & 1) == 0 ? C_FLOOR_A : C_FLOOR_B);
                c.drawRect(tx * 20f, WALL_H + ty * 20f, tx * 20f + 20f, WALL_H + ty * 20f + 20f, p);
            }
        }
        // karpet area ngobrol
        fillRound(c, 105f, 160f, 255f, 228f, 8f, Color.parseColor("#1D1A12"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(Color.parseColor("#4DF2CA50"));
        r.set(109f, 164f, 251f, 224f);
        c.drawRoundRect(r, 6f, 6f, p);

        // dinding
        p.setStyle(Paint.Style.FILL);
        p.setColor(C_WALL);
        c.drawRect(0f, 0f, W, WALL_H, p);
        p.setColor(C_GOLD);
        c.drawRect(0f, WALL_H - 2f, W, WALL_H, p);

        // jendela + skyline
        for (int i = 0; i < 2; i++) {
            float wx = 14f + i * 58f;
            fillRound(c, wx, 7f, wx + 50f, 33f, 3f, Color.parseColor("#162033"));
            p.setColor(Color.parseColor("#22304A"));
            c.drawRect(wx + 4f, 22f, wx + 12f, 33f, p);
            c.drawRect(wx + 15f, 16f, wx + 25f, 33f, p);
            c.drawRect(wx + 28f, 20f, wx + 36f, 33f, p);
            c.drawRect(wx + 38f, 13f, wx + 46f, 33f, p);
            p.setColor(Color.parseColor("#FFE9C176"));
            if (((int) (time * 0.7f) + i) % 2 == 0) c.drawRect(wx + 18f, 20f, wx + 20f, 22f, p);
            else c.drawRect(wx + 40f, 17f, wx + 42f, 19f, p);
        }

        // papan chart candlestick di dinding
        fillRound(c, 136f, 6f, 238f, 36f, 3f, Color.parseColor("#0D0E11"));
        for (int i = 0; i < 9; i++) {
            float v = (float) Math.sin(time * 0.8f + i * 1.3f);
            float v2 = (float) Math.sin(time * 0.8f + (i + 1) * 1.3f);
            boolean up = v2 >= v;
            float cx = 146f + i * 10f;
            float mid = 21f - v * 6f;
            p.setColor(up ? Color.parseColor("#58E7AA") : Color.parseColor("#FFB4AB"));
            p.setStrokeWidth(0.8f);
            p.setStyle(Paint.Style.STROKE);
            c.drawLine(cx, mid - 6f, cx, mid + 6f, p);
            p.setStyle(Paint.Style.FILL);
            c.drawRect(cx - 2f, mid - 3f, cx + 2f, mid + 3f, p);
        }

        // jam dinding
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#292A2D"));
        c.drawCircle(262f, 20f, 11f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1f);
        p.setColor(C_GOLD);
        c.drawCircle(262f, 20f, 11f, p);
        double ms = System.currentTimeMillis() / 1000.0;
        double minA = (ms / 60.0 % 60.0) / 60.0 * Math.PI * 2 - Math.PI / 2;
        double hrA = (ms / 3600.0 % 12.0) / 12.0 * Math.PI * 2 - Math.PI / 2;
        c.drawLine(262f, 20f, 262f + (float) Math.cos(minA) * 8f, 20f + (float) Math.sin(minA) * 8f, p);
        c.drawLine(262f, 20f, 262f + (float) Math.cos(hrA) * 5f, 20f + (float) Math.sin(hrA) * 5f, p);

        // logo
        p.setStyle(Paint.Style.FILL);
        p.setColor(C_GOLD);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(true);
        p.setTextSize(9f);
        c.drawText("ZENTRADE AI", 318f, 22f, p);
        p.setFakeBoldText(false);
        p.setTextSize(5.5f);
        p.setColor(C_TEXT_DIM);
        c.drawText("TRADING FLOOR", 318f, 31f, p);
    }

    private void drawDesk(Canvas c, int i) {
        float x = deskX[i];
        Agent a = i < agents.size() ? agents.get(i) : null;
        boolean occupied = a != null && a.state == State.WORK;

        // meja
        fillRound(c, x, DESK_Y, x + deskW, DESK_Y + DESK_H, 3f, C_DESK);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.8f);
        p.setColor(C_DESK_EDGE);
        r.set(x, DESK_Y, x + deskW, DESK_Y + DESK_H);
        c.drawRoundRect(r, 3f, 3f, p);

        // monitor
        float mx = x + deskW / 2f;
        fillRound(c, mx - 13f, DESK_Y - 12f, mx + 13f, DESK_Y + 4f, 2f, Color.parseColor("#0D0E11"));
        int col = a != null ? a.color : Color.GRAY;
        int alpha = occupied ? 200 + (int) (40 * Math.sin(time * 3 + i)) : 50;
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(alpha, Color.red(col), Color.green(col), Color.blue(col)));
        c.drawRect(mx - 11f, DESK_Y - 10f, mx + 11f, DESK_Y + 2f, p);
        if (occupied) {
            p.setColor(Color.parseColor("#990D0E11"));
            for (int k = 0; k < 3; k++) {
                float len = 6f + 8f * (float) Math.abs(Math.sin(time * 2f + i + k));
                c.drawRect(mx - 9f, DESK_Y - 8f + k * 3.4f, mx - 9f + len, DESK_Y - 6.5f + k * 3.4f, p);
            }
        }
        fillRound(c, mx - 2f, DESK_Y + 4f, mx + 2f, DESK_Y + 7f, 1f, Color.parseColor("#3A3B3F"));

        // keyboard
        fillRound(c, mx - 8f, DESK_Y + 11f, mx + 8f, DESK_Y + 16f, 1.5f, Color.parseColor("#3A3B3F"));

        // label peran di meja
        if (a != null) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(a.color);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(5f);
            p.setFakeBoldText(true);
            c.drawText(a.role, mx, DESK_Y + DESK_H - 2f, p);
            p.setFakeBoldText(false);
        }
    }

    private void drawCoffeeStation(Canvas c) {
        // counter
        fillRound(c, 292f, 56f, 354f, 90f, 3f, Color.parseColor("#2A2420"));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.8f);
        p.setColor(C_DESK_EDGE);
        r.set(292f, 56f, 354f, 90f);
        c.drawRoundRect(r, 3f, 3f, p);

        // mesin kopi
        fillRound(c, 300f, 46f, 318f, 72f, 2f, Color.parseColor("#34353A"));
        fillRound(c, 303f, 50f, 315f, 58f, 1f, Color.parseColor("#0D0E11"));
        boolean brewing = coffeeUser != null && coffeeUser.state == State.BREW;
        p.setStyle(Paint.Style.FILL);
        p.setColor(brewing && ((int) (time * 4)) % 2 == 0 ? Color.parseColor("#FFB4AB")
                : Color.parseColor("#58E7AA"));
        c.drawCircle(309f, 63f, 1.6f, p);
        if (brewing) {
            p.setColor(Color.parseColor("#7A4B25"));
            c.drawRect(308.4f, 66f, 309.6f, 70f, p);
        }
        // cangkir di meja
        fillRound(c, 326f, 70f, 332f, 76f, 1.5f, C_TEXT);
        fillRound(c, 337f, 70f, 343f, 76f, 1.5f, C_TEXT);

        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(5.5f);
        p.setFakeBoldText(true);
        p.setColor(C_TEXT_DIM);
        c.drawText("COFFEE", 330f, 85f, p);
        p.setFakeBoldText(false);
    }

    private void drawDecor(Canvas c) {
        // tanaman
        drawPlant(c, 342f, 212f);
        drawPlant(c, 20f, 212f);
        // meja bundar rapat
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#2A2630"));
        c.drawOval(new RectF(158f, 186f, 202f, 206f), p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(0.8f);
        p.setColor(C_DESK_EDGE);
        c.drawOval(new RectF(158f, 186f, 202f, 206f), p);
    }

    private void drawPlant(Canvas c, float x, float y) {
        fillRound(c, x - 6f, y, x + 6f, y + 12f, 2f, Color.parseColor("#3A2F22"));
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#2F7D57"));
        c.drawCircle(x, y - 3f, 7f, p);
        c.drawCircle(x - 5f, y + 1f, 5f, p);
        c.drawCircle(x + 5f, y + 1f, 5f, p);
        p.setColor(Color.parseColor("#3FA672"));
        c.drawCircle(x, y - 6f, 4f, p);
    }

    private void drawAgent(Canvas c, Agent a) {
        boolean walking = a.state == State.WALK;
        boolean seated = a.state == State.WORK || a.state == State.BREW;
        float bob = walking ? -Math.abs((float) Math.sin(a.walkPhase)) * 1.3f : 0f;
        float ax = a.x;
        float ay = a.y + bob;

        // bayangan
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.parseColor("#44000000"));
        c.drawOval(new RectF(a.x - 6f, a.y + 8f, a.x + 6f, a.y + 11f), p);

        // kursi
        if (a.state == State.WORK) {
            fillRound(c, ax - 8f, ay - 5f, ax + 8f, ay + 7f, 4f, Color.parseColor("#25262B"));
        }

        // kaki
        if (!(a.state == State.WORK)) {
            float sw = walking ? (float) Math.sin(a.walkPhase) * 2.2f : 0f;
            fillRound(c, ax - 4f, ay + 3f + sw * 0.3f, ax - 0.5f, ay + 10f + sw, 1.2f, Color.parseColor("#2B2D33"));
            fillRound(c, ax + 0.5f, ay + 3f - sw * 0.3f, ax + 4f, ay + 10f - sw, 1.2f, Color.parseColor("#2B2D33"));
        }

        // badan
        fillRound(c, ax - 5.5f, ay - 9f, ax + 5.5f, ay + 4f, 3f, a.color);

        // lengan
        float armA = 0f;
        if (a.state == State.WORK) armA = (float) Math.sin(time * 14f + a.name.length()) * 1.2f;
        else if (walking) armA = (float) Math.sin(a.walkPhase) * 2f;
        else if (a.state == State.CHAT && a.bubble != null) armA = (float) Math.sin(time * 8f) * 2f;
        int armColor = darker(a.color);
        if (a.state == State.WORK) {
            fillRound(c, ax - 7f, ay - 10f + armA, ax - 4.5f, ay - 3f, 1.2f, armColor);
            fillRound(c, ax + 4.5f, ay - 10f - armA, ax + 7f, ay - 3f, 1.2f, armColor);
        } else {
            fillRound(c, ax - 7.5f, ay - 8f + armA, ax - 5f, ay + 1f + armA, 1.2f, armColor);
            fillRound(c, ax + 5f, ay - 8f - armA, ax + 7.5f, ay + 1f - armA, 1.2f, armColor);
        }

        // kepala
        float hy = ay - 14f;
        p.setStyle(Paint.Style.FILL);
        p.setColor(C_SKIN);
        c.drawCircle(ax, hy, 5.2f, p);
        p.setColor(Color.parseColor("#2A2118"));
        if (a.facingUp) {
            c.drawCircle(ax, hy - 0.4f, 5.2f, p);
        } else {
            tmpPath.reset();
            r.set(ax - 5.4f, hy - 5.6f, ax + 5.4f, hy + 5.2f);
            tmpPath.addArc(r, 180f, 180f);
            tmpPath.close();
            c.drawPath(tmpPath, p);
            // mata
            p.setColor(Color.parseColor("#1B1B1F"));
            float eo = a.faceDir * 1.2f;
            c.drawCircle(ax - 1.8f + eo, hy + 0.6f, 0.8f, p);
            c.drawCircle(ax + 1.8f + eo, hy + 0.6f, 0.8f, p);
        }

        // cangkir kopi
        if (a.cupTimer > 0f || a.state == State.BREW) {
            float cx = ax + 8f;
            float cy = ay - 2f;
            fillRound(c, cx - 2.2f, cy, cx + 2.2f, cy + 4.5f, 1f, C_TEXT);
            if (a.cupTimer > 0f) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(0.7f);
                p.setColor(Color.parseColor("#99E3E2E6"));
                for (int k = -1; k <= 1; k += 2) {
                    float sx = cx + k * 1f;
                    float off = (float) Math.sin(time * 4f + k) * 1.2f;
                    c.drawLine(sx, cy - 0.5f, sx + off, cy - 3f, p);
                    c.drawLine(sx + off, cy - 3f, sx - off, cy - 5.5f, p);
                }
            }
        }

        // nama
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(5.2f);
        p.setColor(C_TEXT_DIM);
        c.drawText(a.name, ax, ay + 17f, p);
    }

    private void drawBubble(Canvas c, Agent a) {
        if (a.bubble == null) return;
        p.setTextSize(6.2f);
        p.setTextAlign(Paint.Align.CENTER);
        p.setFakeBoldText(false);
        float tw = p.measureText(a.bubble);
        float bw = tw + 10f;
        float bh = 12f;
        float bx = a.x;
        float by = a.y - 30f;
        float left = Math.max(2f, Math.min(W - bw - 2f, bx - bw / 2f));
        float top = Math.max(2f, by - bh);
        p.setStyle(Paint.Style.FILL);
        p.setColor(C_TEXT);
        r.set(left, top, left + bw, top + bh);
        c.drawRoundRect(r, 4f, 4f, p);
        float tipX = Math.max(left + 5f, Math.min(left + bw - 5f, bx));
        tmpPath.reset();
        tmpPath.moveTo(tipX - 2.5f, top + bh - 0.3f);
        tmpPath.lineTo(tipX + 2.5f, top + bh - 0.3f);
        tmpPath.lineTo(bx, top + bh + 4f);
        tmpPath.close();
        c.drawPath(tmpPath, p);
        p.setColor(Color.parseColor("#1B1B1F"));
        c.drawText(a.bubble, left + bw / 2f, top + 8.3f, p);
    }

    private void drawStatusChip(Canvas c) {
        String label = setupActive ? "SETUP AKTIF - semua di meja" : "IDLE - agent santai";
        int col = setupActive ? Color.parseColor("#58E7AA") : C_GOLD;
        p.setTextSize(5.8f);
        p.setFakeBoldText(true);
        float tw = p.measureText(label);
        float w = tw + 18f;
        fillRound(c, 6f, 224f, 6f + w, 236f, 6f, Color.parseColor("#E61B1B1F"));
        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        c.drawCircle(12.5f, 230f, 2f, p);
        p.setTextAlign(Paint.Align.LEFT);
        c.drawText(label, 18f, 232f, p);
        p.setFakeBoldText(false);
    }

    private static int darker(int color) {
        return Color.argb(Color.alpha(color),
                (int) (Color.red(color) * 0.72f),
                (int) (Color.green(color) * 0.72f),
                (int) (Color.blue(color) * 0.72f));
    }
}