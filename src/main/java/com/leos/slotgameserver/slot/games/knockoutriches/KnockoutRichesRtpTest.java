package com.leos.slotgameserver.slot.games.knockoutriches;

import com.leos.slotgameserver.slot.core.PlayMode;
import com.leos.slotgameserver.slot.core.result.SpinResult;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Knockout Riches RTP 測試
 * 可直接用 main 執行，不需要啟動 Spring Boot
 */
public class KnockoutRichesRtpTest {
    
    private static final DecimalFormat df = new DecimalFormat("#,###");
    private static final DecimalFormat pf = new DecimalFormat("0.00%");
    
    public static void main(String[] args) {
        // === 測試參數 ===
        long totalRounds = 100_000_000L;  // 一億次
        long lineBet = 1;
        int prob = 96;
        PlayMode playMode = PlayMode.NORMAL;
        
        // 多執行緒加速
        int threads = Runtime.getRuntime().availableProcessors();
        long roundsPerThread = totalRounds / threads;
        
        System.out.println("=".repeat(60));
        System.out.println("🎰 Knockout Riches 9 Icon - RTP Test");
        System.out.println("=".repeat(60));
        System.out.println("總轉數: " + df.format(totalRounds));
        System.out.println("線注: " + lineBet);
        System.out.println("機率: " + prob);
        System.out.println("模式: " + playMode.getName());
        System.out.println("執行緒: " + threads);
        System.out.println("=".repeat(60));
        
        AtomicLong totalBet = new AtomicLong(0);
        AtomicLong totalWin = new AtomicLong(0);
        AtomicLong freeCount = new AtomicLong(0);
        AtomicLong maxWin = new AtomicLong(0);
        AtomicLong completedRounds = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        // 啟動多執行緒
        Thread[] workers = new Thread[threads];
        for (int t = 0; t < threads; t++) {
            workers[t] = new Thread(() -> {
                KnockoutRichesGame game = new KnockoutRichesGame();
                long localTotalBet = 0;
                long localTotalWin = 0;
                long localFreeCount = 0;
                long localMaxWin = 0;
                
                for (long i = 0; i < roundsPerThread; i++) {
                    SpinResult result = game.spin(lineBet, prob, playMode);
                    
                    localTotalBet += result.getTotalBet();
                    localTotalWin += result.getTotalWin();
                    
                    if (result.isTriggeredFree()) {
                        localFreeCount++;
                    }
                    
                    if (result.getTotalWin() > localMaxWin) {
                        localMaxWin = result.getTotalWin();
                    }
                    
                    // 每 100 萬次輸出進度
                    if ((i + 1) % 1_000_000 == 0) {
                        long completed = completedRounds.addAndGet(1_000_000);
                        double progress = (double) completed / totalRounds * 100;
                        System.out.printf("\r進度: %.1f%% (%s/%s)", 
                                progress, df.format(completed), df.format(totalRounds));
                    }
                }
                
                totalBet.addAndGet(localTotalBet);
                totalWin.addAndGet(localTotalWin);
                freeCount.addAndGet(localFreeCount);
                
                final long finalLocalMaxWin = localMaxWin;
                maxWin.updateAndGet(v -> Math.max(v, finalLocalMaxWin));
            });
            workers[t].start();
        }
        
        // 等待所有執行緒完成
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        long endTime = System.currentTimeMillis();
        double durationSec = (endTime - startTime) / 1000.0;
        
        // 計算結果
        double rtp = (double) totalWin.get() / totalBet.get();
        double freeRate = (double) freeCount.get() / totalRounds;
        double rps = totalRounds / durationSec;
        
        System.out.println("\n");
        System.out.println("=".repeat(60));
        System.out.println("📊 結果");
        System.out.println("=".repeat(60));
        System.out.println("總下注: " + df.format(totalBet.get()));
        System.out.println("總贏分: " + df.format(totalWin.get()));
        System.out.println("RTP: " + pf.format(rtp));
        System.out.println("-".repeat(60));
        System.out.println("免費觸發次數: " + df.format(freeCount.get()));
        System.out.println("免費觸發率: " + pf.format(freeRate));
        System.out.println("最大單次贏分: " + df.format(maxWin.get()) + "x");
        System.out.println("-".repeat(60));
        System.out.println("耗時: " + String.format("%.2f", durationSec) + " 秒");
        System.out.println("速度: " + df.format((long) rps) + " 轉/秒");
        System.out.println("=".repeat(60));
    }
}
