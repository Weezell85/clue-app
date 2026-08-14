package com.cluecompanion.game;
import java.util.*; import org.junit.jupiter.api.Test; import static org.assertj.core.api.Assertions.*;
class GameTest {
 @Test void onlyHostCanStartWithThreePlayers(){Game g=new Game("ABCDE","Host",new Random(1));UUID host=g.players().getFirst().id();assertThatThrownBy(()->g.start(host)).hasMessageContaining("3");g.addPlayer("Two");g.addPlayer("Three");assertThatThrownBy(()->g.start(g.players().get(1).id())).hasMessageContaining("host");g.start(host);assertThat(g.status()).isEqualTo(Game.Status.PLAYING);assertThat(g.players()).extracting(p->p.hand().size()).containsOnly(6);}
 @Test void capsLobbyAtSix(){Game g=new Game("ABCDE","Host",new Random(1));for(int i=2;i<=6;i++)g.addPlayer("Player "+i);assertThatThrownBy(()->g.addPlayer("Seven")).hasMessageContaining("full");}
 @Test void passAdvancesTurn(){Game g=started();UUID first=g.currentPlayer().id();g.noRoom(first);g.pass(first);assertThat(g.currentPlayer().id()).isNotEqualTo(first);assertThat(g.phase()).isEqualTo(Game.Phase.ACTION);}
 private Game started(){Game g=new Game("ABCDE","Host",new Random(2));g.addPlayer("Two");g.addPlayer("Three");g.start(g.players().getFirst().id());return g;}
}
