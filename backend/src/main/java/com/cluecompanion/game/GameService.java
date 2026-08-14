package com.cluecompanion.game;
import java.security.SecureRandom; import java.util.*; import java.util.concurrent.ConcurrentHashMap; import org.springframework.stereotype.Service;
@Service public class GameService {
 private static final String CHARS="ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; private final Map<String,Game> games=new ConcurrentHashMap<>();private final SecureRandom random=new SecureRandom();
 public Game create(String name){String code;do{code=code();}while(games.containsKey(code));Game g=new Game(code,name);games.put(code,g);return g;}
 public Game get(String code){Game g=games.get(code.toUpperCase(Locale.ROOT));if(g==null)throw new GameException("Game not found");return g;}
 private String code(){StringBuilder s=new StringBuilder();for(int i=0;i<5;i++)s.append(CHARS.charAt(random.nextInt(CHARS.length())));return s.toString();}
}
