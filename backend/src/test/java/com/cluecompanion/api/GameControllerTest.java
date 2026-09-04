package com.cluecompanion.api;

import com.cluecompanion.game.GameService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameControllerTest {
 @Test void authenticatedResponsesKeepThePlayersSessionToken(){
  GameController controller=new GameController(new GameService());
  GameController.View created=controller.create(new GameController.NameRequest("Host"));

  GameController.View refreshed=controller.get(created.code(),"Bearer "+created.playerToken());

  assertThat(refreshed.playerToken()).isEqualTo(created.playerToken());
 }
}
