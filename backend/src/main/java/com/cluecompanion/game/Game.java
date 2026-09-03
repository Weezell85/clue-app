package com.cluecompanion.game;

import java.security.SecureRandom;
import java.util.*;
import static com.cluecompanion.game.Card.Type.*;

public final class Game {
 public enum Status { LOBBY, PLAYING, FINISHED } public enum Phase { ROOM, ACTION, DISPROVING, DECISION }
 public record Player(UUID id,UUID token,String name,boolean host,List<Card> hand,boolean eliminated){}
 public record Event(String message,UUID privateFor,Card privateCard){}
 public record Suggestion(Card suspect,Card weapon,Card room){ public List<Card> cards(){return List.of(suspect,weapon,room);} }
 private final String code; private final List<Player> players=new ArrayList<>(); private final List<Event> events=new ArrayList<>(); private final Random random;
 private Status status=Status.LOBBY; private Phase phase=Phase.ROOM; private List<Card> solution=List.of(); private int turn; private Suggestion suggestion; private Card turnRoom; private int responder; private UUID winner;
 Game(String code,String host){this(code,host,new SecureRandom());}
 Game(String code,String host,Random random){this.code=code;this.random=random;add(host,true);}
 public synchronized Player addPlayer(String name){return add(name,false);}
 private Player add(String raw,boolean host){check(status==Status.LOBBY,"Game has already started");check(players.size()<6,"Game is full");String name=raw==null?"":raw.trim();check(!name.isBlank()&&name.length()<=30,"Name must be 1–30 characters");check(players.stream().noneMatch(p->p.name().equalsIgnoreCase(name)),"Name is already in use");Player p=new Player(UUID.randomUUID(),UUID.randomUUID(),name,host,new ArrayList<>(),false);players.add(p);events.add(new Event(name+" joined the game",null,null));return p;}
 public synchronized void start(UUID actor){check(status==Status.LOBBY,"Game has already started");check(players.size()>=3,"At least 3 players are required");check(player(actor).host(),"Only the host can start the game");List<Card>d=new ArrayList<>(Deck.cards());solution=List.of(remove(d,SUSPECT),remove(d,WEAPON),remove(d,ROOM));Collections.shuffle(d,random);for(int i=0;i<d.size();i++)players.get(i%players.size()).hand().add(d.get(i));status=Status.PLAYING;events.add(new Event("Cards dealt. "+players.getFirst().name()+" goes first",null,null));}
 private Card remove(List<Card>d,Card.Type t){List<Card>m=d.stream().filter(c->c.type()==t).toList();Card c=m.get(random.nextInt(m.size()));d.remove(c);return c;}
 public synchronized void enterRoom(UUID actor,Card room){requireTurn(actor);check(phase==Phase.ROOM,"Room movement has already been recorded");check(room!=null&&room.type()==ROOM&&Deck.cards().contains(room),"Select a valid room");turnRoom=room;phase=Phase.ACTION;events.add(new Event(player(actor).name()+" made it into the "+room.name(),null,null));}
 public synchronized void suggest(UUID actor,Suggestion s){requireTurn(actor);check(phase==Phase.ACTION,"A suggestion cannot be made now");validate(s);check(s.room().equals(turnRoom),"The suggestion must use the room you entered");suggestion=s;responder=next(turn);phase=Phase.DISPROVING;events.add(new Event(player(actor).name()+" suggested "+s.suspect().name()+", "+s.weapon().name()+" in the "+s.room().name(),null,null));}
 public synchronized void respond(UUID actor,Card card){check(phase==Phase.DISPROVING,"No suggestion needs a response");check(players.get(responder).id().equals(actor),"It is not your response");List<Card>m=matches(players.get(responder));if(card!=null)check(m.contains(card),"Reveal a matching card");else check(m.isEmpty(),"You must reveal one matching card");if(card!=null){events.add(new Event(players.get(responder).name()+" had one of the cards that "+players.get(turn).name()+" suggested",null,null));events.add(new Event("Revealed card: "+card.name(),players.get(turn).id(),card));phase=Phase.DECISION;}else{events.add(new Event(players.get(responder).name()+" did not have any of the suggested cards",null,null));responder=next(responder);if(responder==turn){events.add(new Event("No player could disprove the suggestion",null,null));phase=Phase.DECISION;}}}
 private List<Card> matches(Player p){return p.hand().stream().filter(suggestion.cards()::contains).toList();}
 public synchronized void noRoom(UUID actor){requireTurn(actor);check(phase==Phase.ROOM,"Room movement has already been recorded");phase=Phase.DECISION;events.add(new Event(player(actor).name()+" did not make it into a room",null,null));}
 public synchronized boolean accuse(UUID actor,Suggestion a){requireTurn(actor);check(phase==Phase.DECISION,"A final accusation cannot be made now");validate(a);boolean correct=new HashSet<>(solution).equals(new HashSet<>(a.cards()));String cards=a.suspect().name()+", "+a.weapon().name()+", "+a.room().name();if(correct){winner=actor;status=Status.FINISHED;events.add(new Event(player(actor).name()+" correctly accused "+cards+". The game is over!",null,null));}else{events.add(new Event(player(actor).name()+" incorrectly accused "+cards+" and is out of the turn order.",null,null));Player p=players.get(turn);players.set(turn,new Player(p.id(),p.token(),p.name(),p.host(),p.hand(),true));advance();}return correct;}
 public synchronized void pass(UUID actor){requireTurn(actor);check(phase==Phase.DECISION,"The turn cannot be finished now");advance();}
 private void advance(){do{turn=next(turn);}while(players.get(turn).eliminated()&&players.stream().anyMatch(p->!p.eliminated()));suggestion=null;turnRoom=null;phase=Phase.ROOM;events.add(new Event("It is now "+players.get(turn).name()+"'s turn",null,null));}
 private void validate(Suggestion s){check(s!=null&&s.suspect().type()==SUSPECT&&s.weapon().type()==WEAPON&&s.room().type()==ROOM&&Deck.cards().containsAll(s.cards()),"Select valid cards");}
 private void requireTurn(UUID id){check(status==Status.PLAYING,"Game is not in progress");check(players.get(turn).id().equals(id),"It is not your turn");check(!player(id).eliminated(),"You have been eliminated");}
 private Player player(UUID id){return players.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new GameException("Unknown player"));}
 public Player authenticate(UUID token){return players.stream().filter(p->p.token().equals(token)).findFirst().orElseThrow(InvalidSessionException::new);}
 private int next(int i){return(i+1)%players.size();} private void check(boolean ok,String message){if(!ok)throw new GameException(message);}
 public String code(){return code;} public List<Player> players(){return List.copyOf(players);} public Status status(){return status;} public Phase phase(){return phase;} public Player currentPlayer(){return players.get(turn);} public Player currentResponder(){return phase==Phase.DISPROVING?players.get(responder):null;} public UUID winner(){return winner;} public List<Card> solution(){return status==Status.FINISHED?solution:List.of();} public List<Event> eventsFor(UUID id){return events.stream().filter(e->e.privateFor()==null||e.privateFor().equals(id)).toList();}
 public Suggestion suggestion(){return suggestion;}
 public Card turnRoom(){return turnRoom;}
}
