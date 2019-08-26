package com.epgpbot.armory.transport;

import java.util.ArrayList;
import java.util.List;

public class ArmoryResponse {
  public ArmoryRequest request;
  public boolean didSucceed;
  public List<String> errors;
  public int updatedCharacterCount;
  public int totalCharacterCount;
  public List<ArmoryAPI.Character> characters;

  public ArmoryResponse(ArmoryRequest request) {
    this.request = request;
    this.didSucceed = false;
    this.errors = new ArrayList<String>();
    this.updatedCharacterCount = 0;
    this.totalCharacterCount = 0;
    this.characters = new ArrayList<>();
  }
}
