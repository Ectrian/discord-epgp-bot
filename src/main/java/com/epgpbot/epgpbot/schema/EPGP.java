package com.epgpbot.epgpbot.schema;

import com.epgpbot.config.Config;

public class EPGP {
  private final Config config;

  public EPGP(Config config) {
    this.config = config;
  }

  public long baseGP() {
    return config.epgp_base_gp;
  }

  public double priority(long ep, long gp, long baseGp) {
    return ep / (baseGp + (double)gp);
  }

  public double priority(long ep, long gp) {
    return priority(ep, gp, config.epgp_base_gp);
  }

  public long decayEP(long ep, long decayRate) {
    double rate = decayRate / 100.0;
    long epLost = (long) Math.floor(ep * rate);
    return ep - epLost;
  }

  public long decayEP(long ep) {
    return decayEP(ep, config.epgp_decay_rate);
  }

  public long decayGP(long gp, long decayRate, long decayBaseGp) {
    double rate = decayRate / 100.0;
    long gpLost = (long) Math.floor((decayBaseGp + gp) * rate);
    return Math.max(0, gp - gpLost);
  }

  public long decayGP(long gp) {
    return decayGP(gp, config.epgp_decay_rate, config.epgp_decay_base_gp);
  }

  public long decayRate() {
    return config.epgp_decay_rate;
  }

  public long decayBaseGP() {
    return config.epgp_decay_base_gp;
  }
}
