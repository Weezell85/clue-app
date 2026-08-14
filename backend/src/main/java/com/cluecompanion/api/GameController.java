package com.cluecompanion.api;
import com.cluecompanion.game.*; import jakarta.validation.Valid; import jakarta.validation.constraints.NotBlank; import java.util.*; import org.springframework.http.HttpStatus; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/games") public class GameController {
 private final GameService service; public GameController(GameService service){this.service=service;}
 public record NameRequest(@NotBlank String name){} public record CardDto(Card.Type type,String name){} public record Selection(CardDto suspect,CardDto weapon,CardDto room){} public record Reveal(CardDto card){}
 public record PlayerDto(UUID id,String name,boolean host,boolean eliminated,int cardCount){} public record EventDto(String message,CardDto revealedCard){}
 public record View(String code,Game.Status status,Game.Phase phase,List<PlayerDto> players,UUID currentPlayerId,UUID responderId,UUID winnerId,List<CardDto> hand,List<EventDto> events){}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@Valid @RequestBody NameRequest r){Game g=service.create(r.name());return view(g,g.players().getFirst().id());}
 @PostMapping("/{code}/players") public View join(@PathVariable String code,@Valid @RequestBody NameRequest r){Game g=service.get(code);Game.Player p=g.addPlayer(r.name());return view(g,p.id());}
 @GetMapping("/{code}") public View get(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id){return view(service.get(code),id);}
 @PostMapping("/{code}/start") public View start(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id){Game g=service.get(code);g.start(id);return view(g,id);}
 @PostMapping("/{code}/suggestions") public View suggest(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id,@RequestBody Selection s){Game g=service.get(code);g.suggest(id,selection(s));return view(g,id);}
 @PostMapping("/{code}/suggestions/respond") public View respond(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id,@RequestBody Reveal r){Game g=service.get(code);g.respond(id,r.card()==null?null:card(r.card()));return view(g,id);}
 @PostMapping("/{code}/no-room") public View noRoom(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id){Game g=service.get(code);g.noRoom(id);return view(g,id);}
 @PostMapping("/{code}/accusations") public View accuse(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id,@RequestBody Selection s){Game g=service.get(code);g.accuse(id,selection(s));return view(g,id);}
 @PostMapping("/{code}/pass") public View pass(@PathVariable String code,@RequestHeader("X-Player-Id") UUID id){Game g=service.get(code);g.pass(id);return view(g,id);}
 private Game.Suggestion selection(Selection s){return new Game.Suggestion(card(s.suspect()),card(s.weapon()),card(s.room()));} private Card card(CardDto c){return new Card(c.type(),c.name());} private CardDto dto(Card c){return c==null?null:new CardDto(c.type(),c.name());}
 private View view(Game g,UUID id){Game.Player me=g.players().stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new GameException("Unknown player"));return new View(g.code(),g.status(),g.phase(),g.players().stream().map(p->new PlayerDto(p.id(),p.name(),p.host(),p.eliminated(),p.hand().size())).toList(),g.currentPlayer().id(),g.currentResponder()==null?null:g.currentResponder().id(),g.winner(),me.hand().stream().map(this::dto).toList(),g.eventsFor(id).stream().map(e->new EventDto(e.message(),dto(e.privateCard()))).toList());}
}
