package br.edu.motionbound.ui;

import br.edu.motionbound.game.PotionEffect;

/** Field notes shown by the pause menu; viewing a note never changes the expedition. */
final class FieldGuide {
    record Entry(String name,String note,String stat,String behavior,String advice,int enemy,PotionEffect potion) { }
    static final Entry[] ENEMIES={
        new Entry("Slime","Salto curto","2 de vida / golpe: 1 coração",
            "Para, prepara o salto e avança na direção marcada. O destino fica definido durante a preparação.",
            "Saia da trajetória quando ele se encolher. Ataque depois do salto ou interrompa a preparação com um golpe.",0,null),
        new Entry("Esqueleto","Golpe frontal","3 de vida / golpe: 1 coração",
            "Persegue o explorador e prepara um golpe à frente do corpo. Não atinge quem sai do alcance ou passa para trás dele.",
            "Contorne pelos lados. Acerte durante a preparação e recue; evite trocar golpes parado na frente dele.",1,null),
        new Entry("Mago roxo","Veneno e raios","2 de vida / raio: meio coração",
            "Arremessa dois frascos verdes. Depois dispara cinco raios, um após o outro, em um leque de 45 graus. A direção fica fixa; os raios não perseguem você.",
            "Saia dos círculos verdes e atravesse o leque para o lado. Use a magia para afastar o mago e interromper sua sequência.",2,null),
        new Entry("Bruxo","Lentidão e fogo","3 de vida / fogo: 2 corações por dose",
            "Combina um frasco cinza com três vermelhos em leque de 45 graus. Se você chegar perto, reage com um cinza rápido que pode acertar durante o voo.",
            "Desvie para o lado e evite as áreas no chão: duram 5 segundos. A magia cria distância; a cura remove a queimadura, mas não a lentidão.",3,null),
        new Entry("Ciclope","Impacto em área","4 de vida / golpe: 1 coração",
            "Resiste a mais golpes e prepara um impacto circular ao redor do corpo. O círculo no chão mostra a área perigosa.",
            "Afaste-se enquanto ele ergue a arma. Aproveite a recuperação para atacar; a magia também interrompe sua preparação.",4,null),
        new Entry("Fantasma","Fenda e investida","2 de vida / golpe: meio coração",
            "Flutua ao se aproximar. Nas fases 4 e 5, pode lançar um frasco azul que teleporta você e invoca três morcegos. Depois volta ao combate próximo.",
            "Saia do círculo azul. Cada fantasma tenta uma fenda, até duas por fase. Se for atingido, aproveite a breve proteção na chegada e observe os morcegos.",5,null),
        new Entry("Morcego","Mergulho rápido","1 de vida / golpe: meio coração",
            "Surge em grupos de três após uma fenda azul. Bate asas, prepara um mergulho e avança. Só começa a agir após a animação de aparição.",
            "Mantenha espaço e ataque após o mergulho. Também precisa ser derrotado para liberar a saída e não deixa corações ao morrer.",6,null)
    };
    static final Entry[] POTIONS={
        new Entry("Frasco de fogo","Chamas e brasas no chão","Área: 5s / dose: 4 pulsos, 2 corações",
            "Cria fogo brilhante com brasas por 5 segundos, mesmo se cair em você. O impacto ou pisar nas chamas aplica quatro pulsos de meio coração, um por segundo.",
            "Sair do fogo não cancela a queimadura. Uma dose ativa não reinicia nem acumula. Após acabar ou ser curada, pisar no fogo restante pode aplicar outra dose.",-1,PotionEffect.FIRE),
        new Entry("Frasco de veneno","Poça verde borbulhante","Área: 5s / dose: 4 pulsos, 2 corações",
            "Deixa uma poça verde brilhante e borbulhante por 5 segundos, mesmo no impacto direto. O impacto ou pisar nela aplica quatro pulsos de meio coração.",
            "O veneno continua após sair da poça. Não reinicia durante uma dose ativa; pode voltar depois que ela acabar. Saia da área antes de usar a cura. Fogo e veneno não acumulam.",-1,PotionEffect.POISON),
        new Entry("Frasco de lentidão","Névoa cinza no chão","Área: 5s / lentidão: 4s / velocidade: 60%",
            "Cria uma área de névoa cinza por 5 segundos, mesmo se acertar você. O impacto ou atravessar a névoa reduz sua velocidade para 60% por 4 segundos.",
            "A lentidão não reinicia enquanto está ativa, mas pode voltar após terminar se você continuar na área. A cura não a remove. Desvie do frasco rápido e use a magia para ganhar espaço.",-1,PotionEffect.SLOW),
        new Entry("Frasco de fenda","Redemoinho azul no chão","Área: 5s / 3 morcegos na primeira ativação",
            "Cria um redemoinho azul por 5 segundos, mesmo no impacto direto. O impacto ou entrar nele teleporta você. Partículas são puxadas ao centro da fenda.",
            "Pode teleportar de novo após a chegada e a proteção breve. Cada fenda invoca 3 morcegos só na primeira ativação, até 6 extras por fase. Saia da área azul e derrote os morcegos.",-1,PotionEffect.RIFT)
    };
    private FieldGuide() { }
}
