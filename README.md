# DuelArena — 1v1 決鬥插件

作者:tsukieru
適用:Purpur 26.2 (Java 25)

## 這個插件做了什麼

- `/duel <玩家> <explosive|blade>` 邀請對方在指定類型的場地決鬥,對方要 `/duel accept` 才會開始。
- 開始後雙方會被傳送到場地各自的重生點(對應你截圖裡的綠寶石磚位置)。
- **爆炸場(explosive)**:可以用水晶、重生錨、TNT 礦車。
- **打刀場(blade)**:禁止放置/使用水晶、重生錨、TNT 礦車。
- 場地的地板/牆壁**永遠打不掉**,但決鬥雙方可以自由放置方塊(例如放障礙物、補牆)。
  判斷邏輯很單純:只要是「決鬥開始後,由參賽者自己放上去的方塊」就可以再被打掉/被爆炸炸掉,
  其他任何方塊(場地本體)一律禁止破壞 —— 所以不管場地是圓的、方的、什麼形狀都沒差,
  你截圖裡的圓頂競技場完全適用,不需要額外處理形狀問題。
- 決出勝負(一方死亡或斷線視為棄權)後,輸家立刻被傳回決鬥前的位置,
  贏家進入 **60 秒整理時間**(可在 config.yml 調整),可以繼續打掉場上自己放的方塊回收掉落物。
- 整理時間結束,或贏家自己輸入 `/duel leave` 提早離開:
  - 場上還沒被回收的「玩家放置方塊」會直接消失(不會噴掉落物)
  - 場地內殘留的掉落物會被清除
  - 贏家被傳回決鬥前的位置

## 指令總覽

### 玩家指令 `/duel`
| 指令 | 說明 |
|---|---|
| `/duel <玩家> explosive` | 邀請對方到爆炸場決鬥 |
| `/duel <玩家> blade` | 邀請對方到打刀場決鬥 |
| `/duel accept` | 接受收到的邀請 |
| `/duel deny` | 拒絕收到的邀請 |
| `/duel cancel` | 取消自己送出的邀請 |
| `/duel leave` | (贏家專用)提早結束整理時間 |

### 管理員指令 `/duelarena`(權限 `duelarena.admin`,預設 OP)
1. `/duelarena create 爆炸場 explosive`
2. 站在場地的其中一個角落(對角座標第一點),輸入 `/duelarena setpos1 爆炸場`
3. 站在對角座標第二點,輸入 `/duelarena setpos2 爆炸場`
   （**pos1/pos2 只是拿來框住整個場地範圍的兩個對角座標,不用剛好貼合圓頂形狀,
   只要整個競技場都被包在這個長方體範圍裡面就好**）
4. 站在其中一個玩家要傳送的位置(例如你截圖裡的其中一塊綠寶石磚上面),
   輸入 `/duelarena setspawn1 爆炸場`
5. 站在另一個傳送點,輸入 `/duelarena setspawn2 爆炸場`
6. 對打刀場重複同樣步驟:
   ```
   /duelarena create 打刀場 blade
   /duelarena setpos1 打刀場
   /duelarena setpos2 打刀場
   /duelarena setspawn1 打刀場
   /duelarena setspawn2 打刀場
   ```
7. `/duelarena list` 查看場地清單與設定狀態
8. `/duelarena info <名稱>` 查看細節
9. `/duelarena delete <名稱>` 刪除場地

擁有 `duelarena.admin.bypass` 權限的人(預設 OP)可以無視保護,自由在場地裡蓋東西/維修。

## 編譯方式

這個專案我沒辦法在這裡直接幫你編譯成 .jar,因為我的沙盒環境無法連網下載
Purpur 的 Maven 依賴。請照以下步驟自己編譯:

1. 安裝 **JDK 25**(Purpur 26.2 API 需要)
2. 安裝 Maven
3. 在專案根目錄(有 `pom.xml` 的地方)執行:
   ```
   mvn clean package
   ```
4. 編譯完成後,jar 檔會在 `target/DuelArena.jar`
5. 把它丟進伺服器的 `plugins/` 資料夾,重啟伺服器

> `pom.xml` 裡 Purpur API 的版本號寫的是 `26.2-R0.1-SNAPSHOT`,這是目前
> Purpur 26.x 慣用的命名方式。如果編譯時抓不到這個版本,
> 去 https://repo.purpurmc.org/snapshots/org/purpurmc/purpur/purpur-api/ 看
> 實際存在的資料夾名稱,把 pom.xml 裡的 `<version>` 改成一樣的字串即可,
> 程式碼完全不用動。

## 用 GitHub Actions 自動編譯

專案裡已經放了 `.github/workflows/build.yml`,把整個資料夾推上 GitHub 之後:

1. 到 repo 的 **Actions** 分頁,會看到 `Build` 這個 workflow 自動跑
   (推 commit 到 main/master 分支、開 PR、或手動點 **Run workflow** 都會觸發)
2. 跑完之後在該次執行紀錄的 **Artifacts** 區塊,會有一個叫 `DuelArena` 的壓縮檔可以下載,
   裡面就是編好的 `DuelArena.jar`
3. 下載解壓縮後把 jar 丟進伺服器 `plugins/` 資料夾即可

這樣就不用自己在本機裝 JDK 25 + Maven 了,交給 Actions 的 runner 處理。

## 設定檔 config.yml

```yaml
settings:
  cleanup-seconds: 60          # 贏家整理時間(秒)
  invite-timeout-seconds: 30   # 邀請逾時秒數
  blade-banned-entities:
    - END_CRYSTAL
    - MINECART_TNT
  blade-banned-blocks:
    - RESPAWN_ANCHOR
```

## 之後如果想擴充

- 下注金幣(串接 Vault):可以在 `/duel` 邀請時加金額參數,決鬥開始前扣款,結束後把兩人的錢轉給贏家。
- 勝負紀錄/排行榜(對照你截圖裡的擊殺/死亡/K-D 那個計分板系統):
  可以在 `DuelManager.endFight()` 裡面呼叫你原本記分板插件的 API 加一勝一敗。
- 場地內限制非參賽者進入:目前沒做,如果需要我可以再加傳送/推出邏輯。

有任何要調整的地方(例如想要下注、想要打完自動記錄勝場之類)再跟我說,我可以接著加。
