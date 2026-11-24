package com.nexora.ev;

/** Energy-weighted learner from user-entered completed trips. No vehicle communication. */
public final class TripLearning {
    private double totalKm;
    private double totalWh;
    private int samples;
    public static double energy(double capacityWh, double start, double end) {
        if (!Double.isFinite(capacityWh) || capacityWh <= 0 || !Double.isFinite(start) || !Double.isFinite(end) || start > 100 || end < 0 || end >= start)
            throw new IllegalArgumentException("Invalid charge readings");
        return capacityWh * (start - end) / 100;
    }
    public void add(double km, double wh) {
        if (!Double.isFinite(km) || !Double.isFinite(wh) || km < 1 || km > 1000 || wh <= 0 || wh/km < 5 || wh/km > 250)
            throw new IllegalArgumentException("Check measured distance and charge");
        totalKm += km; totalWh += wh; samples++;
    }
    public int count() { return samples; }
    public double distance() { return totalKm; }
    public double consumption() {
        if (samples == 0) throw new IllegalStateException("No completed trips");
        return totalWh / totalKm;
    }
    public double range(double usableWh, double charge, double reserve) {
        if (!Double.isFinite(usableWh) || usableWh <= 0 || !Double.isFinite(charge) || !Double.isFinite(reserve) || charge < 0 || charge > 100 || reserve < 0 || reserve > 100)
            throw new IllegalArgumentException("Invalid range inputs");
        return usableWh * Math.max(0, charge-reserve) / 100 / consumption();
    }
}
