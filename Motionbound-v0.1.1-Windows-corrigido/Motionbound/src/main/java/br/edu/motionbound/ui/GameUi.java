package br.edu.motionbound.ui;

import br.edu.motionbound.game.DungeonMap;
import br.edu.motionbound.game.GameAssets;
import br.edu.motionbound.input.GameMode;
import br.edu.motionbound.game.MeleeSwing;
import br.edu.motionbound.ui.UiIcons.Icon;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;

/** Pixel dungeon interface: carved stone, bronze fittings and parchment field notes. */
public final class GameUi implements Disposable {
    public enum Screen { MODE_SELECT, REHAB_SETUP, MENU, PLAYING, PAUSED, DEFEAT, VICTORY }
    public enum Action { NONE, RESUME, NEW_GAME, LOAD, SAVE_EXIT, FULLSCREEN, PAUSE,
        CONVENTIONAL, REHABILITATION, MODE_SELECT, START_SIMULATION }
    private enum Page { SUMMARY, SETTINGS, TUTORIAL }
    public static final class View {
        public int hp, maxHp, heals, defeated, total;
        public float stamina, mana, elapsed, x, y, attackCd, magicCd, healCd;
        public boolean hasSave;
        public GameMode mode=GameMode.CONVENTIONAL,savedMode=GameMode.CONVENTIONAL;
        public int attacks,hits,heartsCollected;
        public float pressure,pressureAccuracy,pressureSeconds;
        public String message = "";
        public DungeonMap dungeon;
    }

    private static final Color INK = Color.valueOf("17141e");
    private static final Color PANEL = Color.valueOf("2b2530");
    private static final Color CARD = Color.valueOf("3c3036");
    private static final Color LINE = Color.valueOf("655261");
    private static final Color WHITE = Color.valueOf("f5dfaf");
    private static final Color MUTED = Color.valueOf("b9a18c");
    private static final Color GOLD = Color.valueOf("d7a05b");
    private static final Color MINT = Color.valueOf("a8c87b");
    private static final Color RED = Color.valueOf("ed7a89");
    private static final Color BLUE = Color.valueOf("7fc5ed");
    private static final Color PAPER = Color.valueOf("d7bb87");
    private static final Color PAPER_TEXT = Color.valueOf("48313e");
    private static final Color PAPER_MUTED = Color.valueOf("785d4b");
    private final ExtendViewport viewport = new ExtendViewport(1280, 720);
    private final SpriteBatch batch = new SpriteBatch();
    private final Texture white;
    private final GameAssets art;
    private final UiIcons icons = new UiIcons();
    private final Vector2 pointer = new Vector2();
    private UiFont small, body, heading, display;
    private float density;
    private float width, height;
    private int focus, buttonIndex;
    private Page page = Page.SUMMARY;
    private Screen previous;
    private Action action;

    public GameUi(GameAssets art) {
        this.art = art;
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE); pixel.fill();
        white = new Texture(pixel); pixel.dispose();
    }

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        viewport.update(w, h, true);
        width = viewport.getWorldWidth(); height = viewport.getWorldHeight();
        float next = Gdx.graphics.getBackBufferWidth() / width;
        if (body == null || Math.abs(next - density) > .015f) {
            disposeFonts(); density = next;
            small = new UiFont(22, false, density);
            body = new UiFont(28, true, density);
            heading = new UiFont(52, true, density);
            display = new UiFont(100, true, density);
        }
    }

    /** ESC returns from a subpage first, then resumes the expedition. */
    public boolean backToSummary() {
        if (page == Page.SUMMARY) return false;
        page = Page.SUMMARY; focus = 1;
        return true;
    }

    /** Gameplay never consumes clicks intended for a HUD panel or the pause button. */
    public boolean blocksGameplayPointer() {
        pointer.set(Gdx.input.getX(),Gdx.input.getY());viewport.unproject(pointer);
        float top=height-28,cx=width/2;
        return contains(28,top-154,354,154)||contains(width-248,top-214,220,214)
                ||contains(cx-242,height-103,484,75)||contains(cx-308,28,616,80)
                ||contains(width-181,28,153,44)||contains(20,24,180,60);
    }
    private boolean contains(float x,float y,float w,float h) {
        return pointer.x>=x&&pointer.x<=x+w&&pointer.y>=y&&pointer.y<=y+h;
    }
    private boolean canContinue(View view) {return view.hasSave&&view.savedMode==view.mode;}

    private void modeSelect(View view) {
        float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);
        center(display,"MOTIONBOUND",width/2,y+506,WHITE);
        center(heading,"Escolha sua jornada",width/2,y+414,GOLD);
        center(small,"Duas formas de explorar a mesma cripta.",width/2,y+367,MUTED);
        parchment(x+28,y+144,476,194);
        parchment(x+536,y+144,476,194);
        item(103,x+45,y+278,42);
        label(body,"MODO CONVENCIONAL",x+101,y+317,PAPER_TEXT);
        label(small,"Mouse e teclado. Mira livre em 360 graus.",x+48,y+267,PAPER_MUTED);
        label(small,"Espada, magia e cura com novas animações.",x+48,y+243,PAPER_MUTED);
        if(button(x+49,y+165,434,52,"Jogar com mouse e teclado",true,false))action=Action.CONVENTIONAL;
        icons.draw(batch,Icon.COG,x+553,y+278,42);
        label(body,"MODO REABILITAÇÃO",x+609,y+317,PAPER_TEXT);
        label(small,"Inclinação e aperto. Mira assistida.",x+556,y+267,PAPER_MUTED);
        label(small,"Hardware em preparação. Simulação disponível.",x+556,y+243,PAPER_MUTED);
        if(button(x+557,y+165,434,52,"Preparar reabilitação",false,false))action=Action.REHABILITATION;
        if(button(width/2-205,y+57,410,44,"Sair do jogo",false,true))Gdx.app.exit();
        center(small,"SETAS: navegar   ENTER: escolher   F11: tela cheia",width/2,y+30,MUTED);
        if(!view.message.isEmpty())center(small,view.message,width/2,y-18,RED);
    }

    private void rehabSetup(View view) {
        float x=(width-1040)/2,y=(height-550)/2;
        panel(x,y,1040,550);
        center(small,"MODO REABILITAÇÃO / PREPARAÇÃO",width/2,y+510,MINT);
        center(heading,"A próxima jornada começa aqui",width/2,y+468,WHITE);
        parchment(x+30,y+207,980,197);
        icons.draw(batch,Icon.COG,x+49,y+320,56);
        label(body,"ESP32 + MPU6050 + FSR402",x+125,y+375,PAPER_TEXT);
        label(small,"Inclinação para mover. Aperto para atacar. Mira assistida.",x+125,y+339,PAPER_MUTED);
        label(small,"Conexão USB e calibração com o dispositivo: próxima etapa.",x+52,y+292,PAPER_TEXT);
        label(small,"Você pode testar agora uma SIMULAÇÃO pelo teclado.",x+52,y+262,PAPER_MUTED);
        label(small,"Dados artificiais: os resultados não representam uma avaliação clínica.",x+52,y+237,PAPER_MUTED);
        center(small,"SIMULAÇÃO: I J K L = inclinar / F = apertar / SHIFT + F = pressão alta",width/2,y+184,MUTED);
        float by=y+116;
        if(canContinue(view)) {
            if(button(x+40,by,464,52,"Continuar simulação salva",true,false))action=Action.LOAD;
            if(button(x+536,by,464,52,"Nova simulação",false,false))action=Action.START_SIMULATION;
        } else if(button(width/2-232,by,464,52,"Experimentar simulação",true,false))action=Action.START_SIMULATION;
        if(button(width/2-232,y+48,464,46,"Voltar à escolha de modo",false,false))action=Action.MODE_SELECT;
    }

    public Action draw(Screen screen, View view) {
        action = Action.NONE;
        if (screen != previous) { focus = 0; page = Page.SUMMARY; previous = screen; }
        int count = screen == Screen.PAUSED ? 5 : screen == Screen.MENU ? (canContinue(view) ? 4 : 3)
                : screen == Screen.MODE_SELECT ? 3 : screen == Screen.REHAB_SETUP ? (canContinue(view)?3:2)
                : screen == Screen.PLAYING ? 1 : 2;
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) focus = (focus + 1) % count;
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) focus = (focus + count - 1) % count;
        pointer.set(Gdx.input.getX(), Gdx.input.getY()); viewport.unproject(pointer);
        buttonIndex = 0;
        viewport.apply(); batch.setProjectionMatrix(viewport.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        batch.begin();
        if (screen == Screen.PLAYING) hud(view);
        else {
            rect(0, 0, width, height, INK, .86f);
            for (int i = 0; i < 5; i++) {
                rune(36, height / 2 - 100 + i * 48, GOLD, .4f);
                rune(width - 44, height / 2 - 100 + i * 48, GOLD, .4f);
            }
            if(screen==Screen.MODE_SELECT)modeSelect(view);
            else if(screen==Screen.REHAB_SETUP)rehabSetup(view);
            else if (screen == Screen.PAUSED) pause(view);
            else if (screen == Screen.MENU) welcome(view);
            else result(screen, view);
        }
        batch.end();
        return action;
    }

    private void hud(View v) {
        float top = height - 28;
        panel(28, top - 154, 354, 154);
        item(101, 45, top - 35, 26);
        label(small, v.mode==GameMode.CONVENTIONAL?"EXPLORADOR / MOUSE":"REABILITAÇÃO / DEMO", 80, top - 13, GOLD);
        rect(48, top - 41, 310, 2, LINE);
        label(small, "VIDA", 49, top - 50, RED);
        for (int i = 0; i < v.maxHp; i++) icons.fill(batch, Icon.HEART, 130 + i * 30, top - 77, 28, i < v.hp ? 1 : 0);
        right(small, v.hp + "/" + v.maxHp, 364, top - 51, WHITE);
        icons.draw(batch, Icon.BOLT, 46, top - 111, 28);
        label(small, "VIGOR", 80, top - 89, GOLD);
        for (int i = 0; i < 8; i++) {
            float charge = MathUtils.clamp(v.stamina / 12.5f - i, 0, 1);
            rect(134 + i * 24, top - 109, 18, 19, INK);
            rect(136 + i * 24, top - 107, 14 * charge, 15, GOLD);
            if (charge > 0) rect(136 + i * 24, top - 95, 14 * charge, 3, WHITE);
        }
        right(small, "" + Math.round(v.stamina), 364, top - 89, GOLD);
        icons.draw(batch, Icon.CRYSTAL, 46, top - 145, 28);
        label(small, "MANA", 80, top - 124, BLUE);
        for (int i = 0; i < 4; i++) icons.fill(batch, Icon.CRYSTAL, 139 + i * 44, top - 145, 28, v.mana / 25f - i);
        right(small, "" + Math.round(v.mana), 364, top - 124, BLUE);

        float mx = width - 248;
        panel(mx, top - 214, 220, 214);
        label(small, "MAPA DA CRIPTA", mx + 19, top - 13, GOLD);
        float scale = 2.9f, x0 = mx + 30, y0 = top - 140;
        rect(x0 - 5, y0 - 5, 170, 112, PAPER_MUTED);
        rect(x0 - 2, y0 - 2, 164, 106, PAPER);
        for (int x = 0; x < DungeonMap.WIDTH; x++) for (int y = 0; y < DungeonMap.HEIGHT; y++) {
            if (v.dungeon.isFloor(x, y)) rect(x0 + x * scale, y0 + y * scale, scale, scale, PAPER_MUTED);
        }
        rect(x0 + 47 * scale - 2, y0 + 27 * scale - 2, 6, 6, v.defeated == v.total ? MINT : RED);
        rect(x0 + v.x / 16 * scale - 2, y0 + v.y / 16 * scale - 2, 5, 5, INK);
        label(small, "VOCÊ", mx + 20, top - 154, WHITE);
        right(small, "SAÍDA", mx + 200, top - 154, GOLD);
        rect(mx + 16, top - 178, 188, 1, LINE);
        label(small, "ABATES", mx + 16, top - 180, MUTED);
        right(small, v.defeated + " / " + v.total, mx + 204, top - 180, MINT);

        float cx = width / 2;
        parchment(cx - 242, height - 103, 484, 75);
        icons.draw(batch, Icon.SCROLL, cx - 230, height - 86, 42);
        label(small, v.defeated == v.total ? "PASSAGEM LIBERADA" : "MISSÃO DA CRIPTA", cx - 178, height - 42, PAPER_MUTED);
        label(body, v.defeated == v.total ? "Encontre o baú da saída" : "Derrote os 7 guardiões", cx - 178, height - 65, PAPER_TEXT);

        float start = cx - 308;
        boolean demo=v.mode==GameMode.REHABILITATION;
        skill(start, 28, demo?"F (DEMO)":"MOUSE ESQ.", "Espada", v.attackCd, MeleeSwing.COOLDOWN, GOLD, demo?"Mira assistida":"Mira em 360 graus");
        skill(start + 208, 28, demo?"EM BREVE":"MOUSE DIR.", "Magia", v.magicCd, .75f, BLUE, demo?"Etapa seguinte":"25 de mana");
        skill(start + 416, 28, demo?"COLETA":"ESPAÇO", "Cura", v.healCd, 1.7f, MINT, demo?"Coração: +1":v.heals + " poções");
        label(small, demo?"DADOS SIMULADOS":"EXPEDIÇÃO 01", 28, 66, demo?BLUE:GOLD);
        label(body, time(v.elapsed), 28, 46, WHITE);
        if(demo) {
            panel(width/2-308,121,616,51);
            label(small,"IJKL: inclinar   F: apertar   SHIFT+F: pressão alta",width/2-290,159,MUTED);
            rect(width/2-290,132,150,6,INK);
            rect(width/2-290,132,150*v.pressure,6,v.pressure>.8f?RED:MINT);
            label(small,"Pressão na faixa: "+Math.round(v.pressureAccuracy)+"%",width/2-120,146,MINT);
        }
        if (button(width - 181, 28, 153, 44, "ESC  Menu", false, false)) action = Action.PAUSE;
    }

    private void skill(float x, float y, String key, String name, float cooldown, float max, Color color, String detail) {
        panel(x, y, 200, 80);
        socket(x + 12, y + 16, 48);
        if(key.equals("COLETA"))batch.draw(art.heartTexture,x+21,y+25,30,30);
        else item(name.equals("Espada") ? 103 : name.equals("Magia") ? 116 : 115, x + 12, y + 16, 48);
        if(key.equals("EM BREVE"))icons.draw(batch,Icon.LOCK,x+27,y+28,28);
        if (cooldown > 0) rect(x + 16, y + 20, 40, 40 * MathUtils.clamp(cooldown / max, 0, 1), INK, .75f);
        rect(x + 68, y + 7, 118 * (1 - MathUtils.clamp(cooldown / max, 0, 1)), 3, color);
        label(small, key, x + 72, y + 70, color);
        label(body, name, x + 72, y + 51, WHITE);
        label(small, detail, x + 72, y + 29, MUTED);
    }

    private void pause(View v) {
        float x = (width - 1060) / 2, y = (height - 552) / 2;
        panel(x, y, 1060, 552);
        label(small, "MOTIONBOUND", x + 32, y + 522, GOLD);
        icons.draw(batch, Icon.PAUSE, x + 308, y + 504, 24);
        label(small, "UM RESPIRO NA JORNADA", x + 340, y + 522, MINT);
        right(small, "A CRIPTA ESQUECIDA", x + 1028, y + 522, MUTED);
        rect(x + 294, y + 30, 4, 450, INK);
        rect(x + 298, y + 30, 2, 450, GOLD);
        socket(x + 116, y + 430, 64);
        batch.setColor(Color.WHITE); batch.draw(art.heroPortrait, x + 124, y + 434, 48, 56);
        center(body, "ACAMPAMENTO", x + 148, y + 422, GOLD);
        center(small, "A espada pode descansar.", x + 148, y + 396, MUTED);
        if (button(x + 28, y + 322, 240, 54, "Retomar aventura", true, false)) action = Action.RESUME;
        if (button(x + 28, y + 262, 240, 48, "Expedição", page == Page.SUMMARY, false)) page = Page.SUMMARY;
        if (button(x + 28, y + 208, 240, 48, "Configurações", page == Page.SETTINGS, false)) page = Page.SETTINGS;
        if (button(x + 28, y + 154, 240, 48, "Tutorial", page == Page.TUTORIAL, false)) page = Page.TUTORIAL;
        if (button(x + 28, y + 48, 240, 48, "Salvar e sair", false, true)) action = Action.SAVE_EXIT;
        float content = x + 330;
        parchment(content - 10, y + 55, 716, 435);
        if (page == Page.SUMMARY) summary(v, content, y);
        else if (page == Page.SETTINGS) settings(v,content, y);
        else tutorial(v,content, y);
        center(small, "SETAS: navegar   ENTER: escolher   ESC: voltar   F11: tela cheia", width / 2, y - 20, MUTED);
        if (!v.message.isEmpty()) label(small, v.message, content, y + 36, RED);
    }

    private void summary(View v, float x, float y) {
        label(heading, "Diário da expedição", x + 14, y + 464, PAPER_TEXT);
        label(small, v.mode==GameMode.CONVENTIONAL?"Combate com mira livre / mouse e teclado.":"SIMULAÇÃO DE SENSORES / ainda sem dispositivo conectado.", x + 16, y + 410, PAPER_MUTED);
        journalStat(x + 12, y + 268, "GUARDIÕES", v.defeated + " / " + v.total, Icon.SKULL);
        journalStat(x + 244, y + 268, "TEMPO ATIVO", time(v.elapsed), Icon.CLOCK);
        if(v.mode==GameMode.REHABILITATION)
            journalStat(x+476,y+268,"NA FAIXA",v.pressureSeconds>0?Math.round(v.pressureAccuracy)+"%":"--",Icon.BOLT);
        else journalStat(x + 476, y + 268, "FRASCOS", "0" + v.heals, Icon.HEART);
        ornament(x + 20, y + 258, 650, PAPER_MUTED);
        icons.draw(batch, Icon.SCROLL, x + 12, y + 186, 48);
        label(small, v.defeated == v.total ? "O CAMINHO ESTÁ LIVRE" : "JURAMENTO DO EXPLORADOR", x + 76, y + 236, PAPER_MUTED);
        label(body, v.defeated == v.total ? "Encontre o baú ao nordeste." : "Derrote os " + (v.total - v.defeated) + " guardiões restantes.", x + 76, y + 208, PAPER_TEXT);
        for (int i = 0; i < v.total; i++) icons.fill(batch, Icon.SKULL, x + 78 + i * 36, y + 153, 28, i < v.defeated ? 1 : 0);
        label(small, "VIDA " + v.hp + "/" + v.maxHp + "    VIGOR " + Math.round(v.stamina) + "%    MANA " + Math.round(v.mana) + "%", x + 20, y + 119, PAPER_TEXT);
        String accuracy=v.attacks==0?"--":Math.round(100f*v.hits/v.attacks)+"%";
        label(small, "GOLPES "+v.attacks+"   ACERTOS "+accuracy+"   CORAÇÕES "+v.heartsCollected, x + 20, y + 90, PAPER_MUTED);
    }

    private void journalStat(float x, float y, String title, String value, Icon icon) {
        if(title.equals("FRASCOS"))item(115,x,y+36,42);
        else icons.draw(batch, icon, x, y + 36, 42);
        label(small, title, x + 50, y + 91, PAPER_MUTED);
        label(heading, value, x + 50, y + 62, PAPER_TEXT);
    }

    private void stat(float x, float y, String label, String value, Color color) {
        socket(x + 8, y + 28, 56);
        icons.draw(batch, label.equals("GUARDIÕES") ? Icon.SKULL : Icon.CLOCK, x + 12, y + 32, 48);
        label(small, label, x + 76, y + 90, MUTED);
        label(heading, value, x + 76, y + 60, color);
    }

    private void settings(View view,float x, float y) {
        label(heading, "Ajustes do aventureiro", x + 14, y + 464, PAPER_TEXT);
        label(small, "Novos recursos serão forjados em breve.", x + 16, y + 410, PAPER_MUTED);
        settingCard(x, y + 280, "Áudio", "Volume geral, música e efeitos sonoros.");
        settingCard(x, y + 174, "Arduino / ESP32", "Conexão, sensores e calibração de movimentos.");
        label(body, "Exibição", x + 20, y + 142, PAPER_TEXT);
        label(small, "F11 alterna entre tela cheia e janela.", x + 20, y + 112, PAPER_MUTED);
        label(small, view.mode==GameMode.CONVENTIONAL?"Modo convencional: mouse e teclado.":"Modo reabilitação: simulação de bancada ativa.", x + 20, y + 83, PAPER_MUTED);
    }

    private void settingCard(float x, float y, String title, String description) {
        rect(x + 12, y + 8, 672, 74, PAPER_MUTED, .15f);
        icons.draw(batch, Icon.LOCK, x + 24, y + 25, 36);
        label(body, title, x + 78, y + 72, PAPER_TEXT);
        label(small, description, x + 78, y + 41, PAPER_MUTED);
        right(small, "EM BREVE", x + 666, y + 71, PAPER_MUTED);
    }

    private void tutorial(View view,float x, float y) {
        if(view.mode==GameMode.REHABILITATION) {
            label(heading,"Bancada de sensores",x+14,y+464,PAPER_TEXT);
            label(small,"SIMULAÇÃO: entradas artificiais, sem avaliação clínica.",x+16,y+410,PAPER_MUTED);
            tutorialCard(x,y+286,"I J K L","Inclinação","I: cima / K: baixo / J: esq. / L: dir.",MINT);
            tutorialCard(x+354,y+286,"F","Aperto","Solte e aperte para cada novo golpe.",GOLD);
            tutorialCard(x,y+190,"SHIFT + F","Pressão alta","Demonstra uma leitura fora da faixa.",GOLD);
            tutorialCard(x+354,y+190,"MIRA ASSISTIDA","Espada","Busca alvos na direção da inclinação.",BLUE);
            tutorialCard(x,y+94,"COLETAR","Corações","Inimigos: 30% de chance / cura +1.",MINT);
            tutorialCard(x+354,y+94,"ESC / F11","Pausa / Tela cheia","R reinicia a simulação e os contadores.",MUTED);
            label(small,"USB, calibração real e demais gestos: próxima etapa.",x+16,y+86,PAPER_TEXT);
            return;
        }
        label(heading, "Grimório do explorador", x + 14, y + 464, PAPER_TEXT);
        label(small, "Movimento e mira independentes. Golpes também nas diagonais.", x + 16, y + 410, PAPER_MUTED);
        tutorialCard(x, y + 286, "WASD / SETAS", "Mover", "Explore em oito direções.", MINT);
        tutorialCard(x + 354, y + 286, "SHIFT", "Correr", "Consome vigor; recupera ao descansar.", GOLD);
        tutorialCard(x, y + 190, "MOUSE ESQUERDO", "Espada", "Arco curto na direção do cursor.", GOLD);
        tutorialCard(x + 354, y + 190, "MOUSE DIREITO", "Magia", "Dano leve e recuo; custa 25 de mana.", BLUE);
        tutorialCard(x, y + 94, "ESPAÇO", "Curar", "Poção +2; inimigos: 30% de coração.", MINT);
        tutorialCard(x + 354, y + 94, "ESC / F11", "Pausa / Tela cheia", "R reinicia a fase durante a aventura.", MUTED);
        label(small, "MISSÃO: vença os 7 guardiões e alcance o baú ao nordeste.", x + 16, y + 86, PAPER_TEXT);
    }

    private void tutorialCard(float x, float y, String key, String title, String detail, Color color) {
        rect(x + 10, y + 6, 320, 1, PAPER_MUTED, .4f);
        rect(x + 10, y + 57, small.width(key) + 16, 22, PAPER_TEXT);
        label(small, key, x + 18, y + 77, WHITE);
        label(body, title, x + 12, y + 53, PAPER_TEXT);
        label(small, detail, x + 12, y + 29, PAPER_MUTED);
    }

    private void welcome(View v) {
        float x = (width - 920) / 2, y = (height - 556) / 2;
        panel(x, y, 920, 556);
        item(29, x + 44, y + 379, 64);
        item(29, x + 812, y + 379, 64);
        center(small, "CRÔNICAS DE UM MUNDO ESQUECIDO", width / 2, y + 511, GOLD);
        center(display, "MOTIONBOUND", width / 2 + 3, y + 452, PAPER_TEXT);
        center(display, "MOTIONBOUND", width / 2, y + 456, WHITE);
        center(body, "A CRIPTA ESQUECIDA", width / 2, y + 366, GOLD);
        center(small, "Explore os corredores. Enfrente os guardiões. Encontre a saída.", width / 2, y + 318, MUTED);
        ornament(x + 110, y + 282, 700, GOLD);
        float by = y + 216;
        if (canContinue(v)) {
            if (button(width / 2 - 205, by, 410, 54, "Continuar expedição", true, false)) action = Action.LOAD;
            by -= 64;
        }
        if (button(width / 2 - 205, by, 410, 54, "Nova aventura", !v.hasSave, false)) action = Action.NEW_GAME;
        if (button(width / 2 - 205, by - 58, 410, 44, "Escolher outro modo", false, false)) action=Action.MODE_SELECT;
        if (button(width / 2 - 205, by - 110, 410, 44, "Sair do jogo", false, true)) Gdx.app.exit();
        center(small, v.message.isEmpty()?"MOUSE ESQ.: espada   MOUSE DIR.: magia   ESPAÇO: cura":v.message,
                width / 2, y - 20, v.message.isEmpty()?MUTED:RED);
    }

    private void result(Screen screen, View v) {
        boolean won = screen == Screen.VICTORY;
        float x = (width - 840) / 2, y = (height - 490) / 2;
        panel(x, y, 840, 490);
        rect(x, y + 487, 840, 3, won ? MINT : RED);
        center(small, won ? "EXPEDIÇÃO CONCLUÍDA" : "FIM DA EXPEDIÇÃO", width / 2, y + 446, won ? MINT : RED);
        center(heading, won ? "A cripta foi libertada." : "Sua jornada ainda continua.", width / 2, y + 398, WHITE);
        center(small, won ? "Todos os guardiões caíram. Você encontrou a saída." : "Recupere o fôlego e tente uma nova estratégia.", width / 2, y + 338, MUTED);
        stat(x + 185, y + 185, "GUARDIÕES", v.defeated + " / " + v.total, MINT);
        stat(x + 435, y + 185, "TEMPO ATIVO", time(v.elapsed), GOLD);
        if(v.mode==GameMode.REHABILITATION)
            center(small,"SIMULAÇÃO / pressão na faixa: "+Math.round(v.pressureAccuracy)+"% / dados artificiais",width/2,y+186,BLUE);
        if (button(width / 2 - 205, y + 99, 410, 54, "Jogar novamente", true, false)) action = Action.NEW_GAME;
        if (button(width / 2 - 205, y + 40, 410, 44, "Sair do jogo", false, true)) Gdx.app.exit();
    }

    private boolean button(float x, float y, float w, float h, String text, boolean active, boolean danger) {
        int index = buttonIndex++;
        boolean hover = pointer.x >= x && pointer.x <= x + w && pointer.y >= y && pointer.y <= y + h;
        if (hover && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) focus = index;
        boolean selected = focus == index;
        Color accent = danger ? RED : GOLD;
        cutRect(x + 3, y - 4, w, h, 4, INK);
        cutRect(x, y, w, h, 4, selected || hover ? accent : LINE);
        cutRect(x + 3, y + 3, w - 6, h - 6, 3, selected || hover ? Color.valueOf("654536") : CARD);
        rect(x + 7, y + h - 6, w - 14, 2, selected || hover ? PAPER : LINE);
        rect(x + 7, y + 5, w - 14, 2, INK);
        if (active) rune(x + w - 24, y + h / 2 - 5, accent, 1);
        if (text.contains("aventura") || text.contains("Jogar")) item(103, x + 10, y + (h - 32) / 2, 32);
        else if (text.contains("Continuar")) item(90, x + 10, y + (h - 32) / 2, 32);
        else icons.draw(batch, text.contains("Salvar") || text.contains("Sair") ? Icon.DOOR
                : text.contains("Configurações") ? Icon.COG : text.contains("ESC") ? Icon.PAUSE : Icon.SCROLL,
                x + 10, y + (h - 28) / 2, 28);
        label(body, text, x + 48, y + h / 2 + 11, danger ? RED : selected || active || hover ? WHITE : MUTED);
        // Only the visible, focused action receives keyboard activation.
        return (hover && Gdx.input.isButtonJustPressed(Input.Buttons.LEFT))
                || (selected && (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || (previous != Screen.PLAYING && Gdx.input.isKeyJustPressed(Input.Keys.SPACE))));
    }

    private void panel(float x, float y, float w, float h) {
        cutRect(x + 7, y - 8, w, h, 8, INK);
        cutRect(x, y, w, h, 8, INK);
        cutRect(x + 3, y + 3, w - 6, h - 6, 6, LINE);
        cutRect(x + 7, y + 7, w - 14, h - 14, 4, PANEL);
        rect(x + 12, y + h - 6, w - 24, 2, MUTED);
        rect(x + 12, y + 5, w - 24, 2, Color.valueOf("3f333d"));
        // Masonry seams and bronze rivets are deliberately drawn on the pixel grid.
        for (int i = 48; i < w - 20; i += 64) {
            rect(x + i, y + 3, 2, 5, INK);
            rect(x + i + 12, y + h - 8, 2, 5, INK);
        }
        for (int i = 40; i < h - 12; i += 48) {
            rect(x + 3, y + i, 5, 2, INK); rect(x + w - 8, y + i + 12, 5, 2, INK);
        }
        rivet(x + 9, y + 9); rivet(x + w - 15, y + 9);
        rivet(x + 9, y + h - 15); rivet(x + w - 15, y + h - 15);
    }

    private void parchment(float x, float y, float w, float h) {
        cutRect(x + 5, y - 5, w, h, 6, INK);
        cutRect(x, y, w, h, 6, PAPER_MUTED);
        cutRect(x + 4, y + 4, w - 8, h - 8, 4, PAPER);
        rect(x + 9, y + h - 12, w - 18, 6, WHITE, .45f);
        rect(x + 9, y + 8, w - 18, 4, GOLD, .7f);
        for (int i = 24; i < h - 20; i += 21) {
            rect(x + 7, y + i, 4, 2, PAPER_MUTED, .4f);
            rect(x + w - 15, y + i + 5, 8, 2, PAPER_MUTED, .4f);
        }
        // Rolled ends make quest panels and the field journal read as physical parchment.
        rect(x - 3, y + h - 5, w + 6, 5, GOLD);
        rect(x - 3, y + 2, w + 6, 4, PAPER_MUTED);
    }

    private void socket(float x, float y, float size) {
        cutRect(x, y, size, size, 4, GOLD);
        cutRect(x + 3, y + 3, size - 6, size - 6, 3, INK);
        rect(x + 6, y + 6, size - 12, size - 12, CARD);
    }
    private void item(int index, float x, float y, float size) {
        batch.setColor(Color.WHITE); batch.draw(art.tile(index), x, y, size, size);
    }
    private void rivet(float x, float y) {
        rect(x, y, 6, 6, INK); rect(x, y + 2, 4, 4, GOLD); rect(x, y + 4, 2, 2, WHITE);
    }
    private void rune(float x, float y, Color color, float alpha) {
        rect(x + 4, y, 4, 12, color, alpha); rect(x, y + 4, 12, 4, color, alpha);
        rect(x + 4, y + 4, 4, 4, INK, alpha);
    }
    private void ornament(float x, float y, float w, Color color) {
        rect(x, y + 5, w / 2 - 18, 2, color); rect(x + w / 2 + 18, y + 5, w / 2 - 18, 2, color);
        rune(x + w / 2 - 6, y, color, 1);
    }
    private void cutRect(float x, float y, float w, float h, float cut, Color color) {
        rect(x + cut, y, w - 2 * cut, h, color); rect(x, y + cut, w, h - 2 * cut, color);
    }
    private void rect(float x, float y, float w, float h, Color c) { rect(x, y, w, h, c, 1); }
    private void rect(float x, float y, float w, float h, Color c, float alpha) {
        float x0 = Math.round(x * density) / density, y0 = Math.round(y * density) / density;
        float x1 = Math.round((x + w) * density) / density, y1 = Math.round((y + h) * density) / density;
        batch.setColor(c.r, c.g, c.b, alpha); batch.draw(white, x0, y0, x1 - x0, y1 - y0); batch.setColor(Color.WHITE);
    }
    private void label(UiFont font, String text, float x, float y, Color color) { font.draw(batch, text, x, y, color); }
    private void right(UiFont font, String text, float x, float y, Color color) { label(font, text, x - font.width(text), y, color); }
    private void center(UiFont font, String text, float x, float y, Color color) { label(font, text, x - font.width(text) / 2, y, color); }
    public static String time(float elapsed) { return String.format(java.util.Locale.ROOT, "%02d:%02d", (int)elapsed / 60, (int)elapsed % 60); }
    private void disposeFonts() {
        if (small != null) small.dispose(); if (body != null) body.dispose();
        if (heading != null) heading.dispose(); if (display != null) display.dispose();
    }
    @Override public void dispose() { disposeFonts(); icons.dispose(); white.dispose(); batch.dispose(); }
}
