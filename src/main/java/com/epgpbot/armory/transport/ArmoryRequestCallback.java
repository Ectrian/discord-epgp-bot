package com.epgpbot.armory.transport;

@FunctionalInterface
public interface ArmoryRequestCallback {
  public void execute(ArmoryResponse response);
}
