// WebSocketServer.java

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@ServerEndpoint(value = "/game")
public class WebSocketServer {
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<>());
    // 使用两个单独的映射保证 <Integer, Session> 的双向映射，不采取数组是由于 client 数量动态变化
    private static Map<Integer, Session> idToSessionMap = Collections.synchronizedMap(new HashMap<>());
    private static Map<Session, Integer> sessionToIdMap = new ConcurrentHashMap<>();
    // 需要使用 对局 ID 找到对局的两人姓名
    private static Map<Integer, List<String>> startedGame = new HashMap<>();
    // 记录当前应当下棋的人
    private static Map<Integer, Integer> turns = new HashMap<>();
    
    // cnt 给每个 session 记录 id
    private static Integer cnt = 0;
    private static final Gson gson = new Gson();
    

    @OnOpen
    public void onOpen(Session session) {
        clients.add(session);
        idToSessionMap.put(cnt++, session);
        sessionToIdMap.put(session, cnt - 1);
        System.out.println("New session opened: " + session.getId());
        System.out.println("New session opened and cnt is:" + (cnt - 1));
    }

    @OnClose
    public void onClose(Session session) {
        clients.remove(session);
        Integer clientID = sessionToIdMap.remove(session);
        if (clientID != null) {
          idToSessionMap.remove(clientID);
        }
        System.out.println("Session closed: " + session.getId());
    }

    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        System.out.println("Received message: " + message + " from " + session.getId());
        
        // 对战 id 0-1,2-3...
        Integer clientId = sessionToIdMap.get(session);

        // 根据 clientId 判断奇偶，决定黑白
        int color = clientId % 2;

        int gameId = clientId/2;

        JsonObject jsonObject = gson.fromJson(message, JsonObject.class);
        JsonObject jsonObject2 = gson.fromJson(message, JsonObject.class);
        String msgKind = jsonObject.get("msgKind").getAsString();

        if ("start".equals(msgKind)) {
          String playerName = jsonObject.get("name").getAsString();
          startedGame.computeIfAbsent(gameId, k -> new ArrayList<>()).add(playerName);

          List<String> names = startedGame.get(gameId);
          if (names.size() == 2) {
            System.out.println(names.get(0)+ " vs " + names.get(1));
            JsonObject response = new JsonObject();
            response.addProperty("msgKind", "start");
            response.addProperty("name", names.get(0));
            response.addProperty("opName", names.get(1));
            String jsonResponse = gson.toJson(response);

            // 加入现在的轮次，默认首先进入的先手
            turns.computeIfAbsent(gameId, k -> 0);

            synchronized (clients) {
              Session player1 = idToSessionMap.get(gameId*2);
              Session player2 = idToSessionMap.get(gameId*2 + 1);
              if (player1 != null && player2 != null) {
                  player1.getBasicRemote().sendText(jsonResponse);
                  player2.getBasicRemote().sendText(jsonResponse);
              } 
            }
            System.out.println(jsonResponse);
          }
        }

        else if ("move".equals(msgKind) && startedGame.get(gameId).size() == 2) {
          boolean doMove = true;
          String moveKind = jsonObject.get("moveKind").getAsString();
          Integer turn = turns.get(gameId);
          System.out.println("现在轮到 player:" + turn);
          // 根据 moveKind 和 place 更新棋盘或执行其他操作
          if ("forward".equals(moveKind)) {
            if (color == 0) {
              if (turn == 0) {
                // 将 'X' 放置在指定位置
                jsonObject.addProperty("color", 'X');
                jsonObject2.addProperty("color", 'X');
                jsonObject.addProperty("right", 1);
                jsonObject2.addProperty("right", 0);
                // 切换下棋手
                turns.put(gameId, 1);
              }
              else {
                doMove = false;
                JsonObject wait = new JsonObject();
                wait.addProperty("msgKind", "wait");
                String waitString = gson.toJson(wait);
                synchronized (clients) {
                  session.getBasicRemote().sendText(waitString);
                }
                System.out.println(waitString);
              }
            }
            else {
              if (turn == 1) {
                // 将 'O' 放置在指定位置
                jsonObject.addProperty("color", 'O');
                jsonObject2.addProperty("color", 'O');
                jsonObject.addProperty("right", 0);
                jsonObject2.addProperty("right", 1);
                // 切换下棋手
                turns.put(gameId, 0);
              }
              else {
                doMove = false;
                JsonObject wait = new JsonObject();
                wait.addProperty("msgKind", "wait");
                String waitString = gson.toJson(wait);
                synchronized (clients) {
                  session.getBasicRemote().sendText(waitString);
                }
                System.out.println(waitString);
              }
            }
          } else if ("backward".equals(moveKind)) {
            // 将指定位置清空
            if (turn == 1) {
              turns.put(gameId, 0);
              }
            else
            {
              turns.put(gameId, 1);
            }
            jsonObject.addProperty("color", '\0');
            jsonObject.addProperty("msgkind", "move");
            jsonObject2.addProperty("msgkind", "move");
            jsonObject2.addProperty("color", '\0');
          }
          String response = gson.toJson(jsonObject);
          String response2 = gson.toJson(jsonObject2);

          if (doMove) {
            synchronized (clients) {
              Session player1 = idToSessionMap.get(gameId*2);
              Session player2 = idToSessionMap.get(gameId*2 + 1);
              if (player1 != null && player2 != null) {
                  player1.getBasicRemote().sendText(response);
                  player2.getBasicRemote().sendText(response2);
              } 
            }
            System.out.println(response);
          } 
          }

        else if ("win".equals(msgKind) && startedGame.get(gameId).size() == 2){
            System.out.println("server right");
            Session player1 = idToSessionMap.get(gameId*2);
            Session player2 = idToSessionMap.get(gameId*2 + 1);
            JsonObject win = new JsonObject();
            JsonObject lose = new JsonObject();
            win.addProperty("msgKind", "win");
            lose.addProperty("msgKind", "lose");
            String WIN = gson.toJson(win);
            String LOSE = gson.toJson(lose);
            Integer turn = turns.get(gameId);
            if(turn == 0){        //判断哪位玩家获胜
                 player1.getBasicRemote().sendText(LOSE);
                 player2.getBasicRemote().sendText(WIN);
                 turns.put(gameId, 1);
            }
            else {
                  player2.getBasicRemote().sendText(LOSE);
                  player1.getBasicRemote().sendText(WIN);
                  turns.put(gameId, 0);
            }
        }
        

        
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error on session " + session.getId() + ": " + throwable.getMessage());
    }
}
