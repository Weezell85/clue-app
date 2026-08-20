package com.cluecompanion.game;
import java.util.List;
import static com.cluecompanion.game.Card.Type.*;
public final class Deck {
 private Deck() {}
 public static List<Card> cards() { return List.of(
  new Card(SUSPECT,"Miss Scarlet"),new Card(SUSPECT,"Colonel Mustard"),new Card(SUSPECT,"Mrs. White"),new Card(SUSPECT,"Mr. Green"),new Card(SUSPECT,"Mrs. Peacock"),new Card(SUSPECT,"Professor Plum"),
  new Card(WEAPON,"Candlestick"),new Card(WEAPON,"Dagger"),new Card(WEAPON,"Lead Pipe"),new Card(WEAPON,"Revolver"),new Card(WEAPON,"Rope"),new Card(WEAPON,"Wrench"),
  new Card(ROOM,"Kitchen"),new Card(ROOM,"Ballroom"),new Card(ROOM,"Conservatory"),new Card(ROOM,"Dining Room"),new Card(ROOM,"Billiard Room"),new Card(ROOM,"Library"),new Card(ROOM,"Lounge"),new Card(ROOM,"Hall"),new Card(ROOM,"Study")); }
}
