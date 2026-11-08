package com.example.triduofit.data

import com.example.triduofit.model.AnimationType
import com.example.triduofit.model.EquipmentType
import com.example.triduofit.model.Exercise
import com.example.triduofit.model.SplitType
import com.example.triduofit.model.TargetType
import com.example.triduofit.model.WorkoutSession

class WorkoutProgramRepository {

    // 1. VÉLO ELLIPTIQUE (CARDIO & ÉCHAUFFEMENT) -> TEMPS EN SECONDES
    val exerciseEllipticalWarmup = Exercise(
        id = "elliptical_warmup",
        name = "Échauffement sur Vélo Elliptique",
        equipment = EquipmentType.ELLIPTICAL,
        targetMuscle = "Système Cardio-Vasculaire & Articulations",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 60,
        repUnitLabel = "SECONDES",
        restSeconds = 15,
        voiceCue = "Allure régulière sur le vélo elliptique. Pousse bien avec les jambes et tire sur les bras.",
        postureTip = "Garde le buste droit, respire amplement par le nez et la bouche, échauffe tes épaules.",
        animationType = AnimationType.ELLIPTICAL_CARDIO,
        executionSteps = listOf(
            "1. Monte sur les pédales en tenant fermement les poignées mobiles.",
            "2. Lance le mouvement fluide vers l'avant en poussant sur le talon et la plante du pied.",
            "3. Coordonne la poussée des bras et la traction opposée pour engager tout le corps."
        ),
        commonMistakes = "Ne te penche pas en avant sur la console, garde le dos droit.",
        drawableResName = "ex_elliptical",
        videoResName = "video_elliptical"
    )

    val exerciseEllipticalInterval = Exercise(
        id = "elliptical_interval",
        name = "Accélération Cardio sur Vélo Elliptique",
        equipment = EquipmentType.ELLIPTICAL,
        targetMuscle = "Puissance Aérobie Triathlon",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 45,
        repUnitLabel = "SECONDES",
        restSeconds = 20,
        voiceCue = "Monte la cadence sur l'elliptique ! Rythme soutenu pour simuler une relance en vélo.",
        postureTip = "Engage les fessiers à chaque poussée de pédale, cadence dynamique.",
        animationType = AnimationType.ELLIPTICAL_CARDIO,
        executionSteps = listOf(
            "1. Augmente la fréquence de pédalage pour atteindre 70-80 tours/min.",
            "2. Transfère le poids de corps activement d'un pied sur l'autre.",
            "3. Maintiens les abdos gainés pour stabiliser le tronc."
        ),
        commonMistakes = "Évite de pédaler uniquement sur la pointe des pieds.",
        drawableResName = "ex_elliptical",
        videoResName = "video_elliptical"
    )

    // 2. PLATEAU D'ÉQUILIBRE (PROPRIOCEPTION & CHEVILLES)
    val exerciseBalanceSquat = Exercise(
        id = "balance_board_squats",
        name = "Squats sur Plateau d'Équilibre",
        equipment = EquipmentType.BALANCE_BOARD,
        targetMuscle = "Stabilisateurs Chevilles, Genoux & Quadriceps",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Monte sur le plateau d'équilibre, stabilise le centre et descends en squat contrôlé.",
        postureTip = "Fixe un point devant toi, les micro-oscillations renforcent tes chevilles pour la course.",
        animationType = AnimationType.BALANCE_BOARD_STABILITY,
        executionSteps = listOf(
            "1. Place les pieds écartés de la largeur des épaules sur le plateau rouge.",
            "2. Trouve ton équilibre central sans que les bords ne touchent le sol.",
            "3. Fléchis lentement les genoux à mi-course (demi-squat) en gardant les bras tendus devant.",
            "4. Remonte en contrôlant le retour sans à-coups."
        ),
        commonMistakes = "Ne regarde pas tes pieds, fixe l'horizon pour stabiliser le système vestibulaire.",
        drawableResName = "ex_balance_board",
        videoResName = "video_balance_board"
    )

    val exerciseBalancePlank = Exercise(
        id = "balance_board_plank",
        name = "Planche Mains sur Plateau d'Équilibre",
        equipment = EquipmentType.BALANCE_BOARD,
        targetMuscle = "Gainage Profond & Ceinture Scapulaire",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 40,
        repUnitLabel = "SECONDES",
        restSeconds = 20,
        voiceCue = "Pose les deux mains sur les bords du plateau en position de planche, stabilise sans trembler.",
        postureTip = "Rentre le ventre, serre les fessiers, c'est ce gainage qui t'aligne dans l'eau en natation.",
        animationType = AnimationType.BALANCE_BOARD_STABILITY,
        executionSteps = listOf(
            "1. Mains à plat sur les rebords du plateau, bras tendus sous les épaules.",
            "2. Corps parfaitement rectiligne de la tête aux talons.",
            "3. Absorbe les micro-oscillations avec les muscles profonds du tronc."
        ),
        commonMistakes = "Ne laisse pas le bassin s'affaisser vers le sol.",
        drawableResName = "ex_balance_board",
        videoResName = "video_balance_board"
    )

    // 3. BARRES PARALLÈLES (DIPS, DOS ET TRACTIONS INVERSÉES) -> RÉPÉTITIONS
    val exerciseParallelDips = Exercise(
        id = "parallel_bars_dips",
        name = "Dips sur Barres Parallèles",
        equipment = EquipmentType.PARALLEL_BARS,
        targetMuscle = "Triceps, Pectoraux & Poussée",
        targetType = TargetType.REPETITIONS,
        targetCount = 10,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 25,
        voiceCue = "En appui sur les barres parallèles, fléchis les coudes jusqu'à 90 degrés puis repousse.",
        postureTip = "Baisse bien les épaules, ne creuse pas le dos et contrôle la descente.",
        animationType = AnimationType.PARALLEL_BARS_DIPS,
        executionSteps = listOf(
            "1. Monte en suspension sur les poignées en mousse, bras tendus, buste légèrement incliné.",
            "2. Fléchis les coudes vers l'arrière jusqu'à obtenir un angle droit (90°).",
            "3. Pousse fermement avec les paumes pour revenir en position haute sans claquer les coudes."
        ),
        commonMistakes = "Ne descends pas trop bas sous les 90° pour protéger l'articulation de l'épaule.",
        drawableResName = "ex_parallel_dips",
        videoResName = "video_parallel_dips"
    )

    val exerciseInvertedRow = Exercise(
        id = "parallel_bars_inverted_row",
        name = "Tractions Inversées sous Barres",
        equipment = EquipmentType.PARALLEL_BARS,
        targetMuscle = "Grand Dorsal, Rhomboïdes & Traction Natation",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Glisse-toi sous la barre parallèle, tire la poitrine vers la barre en serrant les omoplates.",
        postureTip = "Maintiens le corps droit en planche inversée, tire avec les coudes vers l'arrière.",
        animationType = AnimationType.PARALLEL_BARS_INVERTED_ROW,
        executionSteps = listOf(
            "1. Installe-toi allongé sous la barre, saisis la barre en pronation (mains écartées un peu plus que les épaules).",
            "2. Talons au sol, corps rigide en planche inversée alignée.",
            "3. Tire la poitrine jusqu'à frôler la barre en resserrant fort les omoplates.",
            "4. Redescends avec lenteur et contrôle."
        ),
        commonMistakes = "Ne casse pas l'alignement des hanches : garde les fessiers contractés.",
        drawableResName = "ex_inverted_row",
        videoResName = "video_inverted_row"
    )

    val exerciseParallelKneeRaises = Exercise(
        id = "parallel_bars_knee_raises",
        name = "Relevés de Genoux Suspendus",
        equipment = EquipmentType.PARALLEL_BARS,
        targetMuscle = "Bas des Abdos & Psoas",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Bras tendus sur les barres parallèles, monte les genoux vers la poitrine sans balancer.",
        postureTip = "Contrôle la descente, ne prends pas d'élan, ce sont les abdominaux qui travaillent.",
        animationType = AnimationType.PARALLEL_BARS_DIPS,
        executionSteps = listOf(
            "1. En suspension sur les barres, stabilise le buste.",
            "2. Enroule le bassin pour amener les genoux à hauteur de poitrine.",
            "3. Marque 1 seconde de pause en haut, puis redescends lentement sans cambrer."
        ),
        commonMistakes = "Évite le balancement d'avant en arrière (aucun élan).",
        drawableResName = "ex_parallel_dips",
        videoResName = "video_parallel_dips"
    )

    // 4. BANC À DORSAUX / LOMBAIRES -> RÉPÉTITIONS
    val exerciseHyperextension = Exercise(
        id = "back_bench_hyperextension",
        name = "Hyperextensions sur Banc à Dorsaux",
        equipment = EquipmentType.BACK_BENCH,
        targetMuscle = "Lombaires & Chaîne Postérieure",
        targetType = TargetType.REPETITIONS,
        targetCount = 15,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Bassin calé sur le banc, descends le buste puis remonte dans l'alignement naturel.",
        postureTip = "Contracte les fessiers en haut, c'est ta cuirasse pour la position aéro sur le vélo.",
        animationType = AnimationType.BACK_BENCH_HYPEREXTENSION,
        executionSteps = listOf(
            "1. Cale les talons sous les boudins arrières, le haut des cuisses en appui sur le coussin.",
            "2. Mains croisées sur la poitrine ou derrière les oreilles, dos plat.",
            "3. Fléchis le buste vers le bas à environ 90° sans arrondir exagérément.",
            "4. Remonte par la force des ischios et des lombaires jusqu'à l'alignement droit parfait (180°)."
        ),
        commonMistakes = "Ne cherche pas à cambrer excessivement en haut : arrête-toi dès que le dos est droit.",
        drawableResName = "ex_hyperextension",
        videoResName = "video_hyperextension"
    )

    val exerciseHyperextensionHold = Exercise(
        id = "back_bench_hold",
        name = "Maintien Isométrique Banc Dorsaux",
        equipment = EquipmentType.BACK_BENCH,
        targetMuscle = "Endurance Paravertébrale Triathlon",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 35,
        repUnitLabel = "SECONDES",
        restSeconds = 25,
        voiceCue = "Maintiens le buste à l'horizontale, bras croisés sur le torse, respire régulièrement.",
        postureTip = "Garde la nuque allongée, ne lâche rien !",
        animationType = AnimationType.BACK_BENCH_HYPEREXTENSION,
        executionSteps = listOf(
            "1. En position haute sur le banc incliné, aligne la tête, le dos et les jambes.",
            "2. Maintiens la posture sans bouger pendant toute la durée du décompte.",
            "3. Respire calmement et profondément."
        ),
        commonMistakes = "Ne relève pas la tête en cassant la nuque : regarde vers le sol à 45°.",
        drawableResName = "ex_hyperextension",
        videoResName = "video_hyperextension"
    )

    // 5. BANC À ABDOMINAUX INCLINÉ ROUGE -> RÉPÉTITIONS
    val exerciseAbCrunch = Exercise(
        id = "ab_bench_crunch",
        name = "Crunchs sur Banc Incliné Rouge",
        equipment = EquipmentType.AB_BENCH,
        targetMuscle = "Grand Droit de l'Abdomen",
        targetType = TargetType.REPETITIONS,
        targetCount = 15,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Pieds calés sous les manchons du banc incliné, enroule le buste sans tirer sur la tête.",
        postureTip = "Expire en montant, garde la tension abdominale constante.",
        animationType = AnimationType.AB_BENCH_CRUNCH,
        executionSteps = listOf(
            "1. Allonge-toi sur le banc rouge incliné, pieds verrouillés sous les manchons supérieurs.",
            "2. Mains posées sur les tempes ou sur la poitrine (ne tire jamais sur la nuque).",
            "3. Enroule le haut du dos en rapprochant les côtes du bassin en expirant.",
            "4. Déroule lentement sans reposer complètement les épaules pour maintenir la tension."
        ),
        commonMistakes = "Ne tire pas sur la tête avec les bras et ne décolle pas le bas du dos brutalement.",
        drawableResName = "ex_ab_crunch",
        videoResName = "video_ab_crunch"
    )

    val exerciseAbLegRaise = Exercise(
        id = "ab_bench_leg_raise",
        name = "Relevés de Bassin sur Banc Incliné",
        equipment = EquipmentType.AB_BENCH,
        targetMuscle = "Abdominaux Sous-Ombilicaux",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Mains agrippées en haut du banc, soulève le bassin vers le haut avec contrôle.",
        postureTip = "Freine la descente pour recruter le bas des abdos au maximum.",
        animationType = AnimationType.AB_BENCH_CRUNCH,
        executionSteps = listOf(
            "1. Allongé tête vers le haut du banc, agrippe fermement la poignée ou le sommet du dossier.",
            "2. Genoux légèrement fléchis, contracte le bas-ventre pour décoller le bassin vers le haut.",
            "3. Redescends millimètre par millimètre avec un contrôle total."
        ),
        commonMistakes = "Ne balance pas les jambes avec élan, le mouvement doit être pur et gainé.",
        drawableResName = "ex_ab_crunch",
        videoResName = "video_ab_crunch"
    )

    // 6. CUBE PLYO EN BOIS -> RÉPÉTITIONS
    val exerciseStepUp = Exercise(
        id = "cube_step_up",
        name = "Step-ups sur Cube en Bois",
        equipment = EquipmentType.PLYO_BOX,
        targetMuscle = "Quadriceps & Fessiers",
        targetType = TargetType.REPETITIONS,
        targetCount = 15,
        repUnitLabel = "PAR JAMBE",
        restSeconds = 20,
        voiceCue = "Pied à plat sur le cube en bois, monte d'un coup puissant et contrôle la descente.",
        postureTip = "Pousse fort sur la jambe d'appui sans donner d'élan avec le pied au sol.",
        animationType = AnimationType.PLYO_BOX_STEP_UP,
        executionSteps = listOf(
            "1. Place un pied entièrement à plat sur le cube en bois (talon bien ancré).",
            "2. Pousse vigoureusement dans le talon de la jambe haute pour te hisser.",
            "3. Monte le genou opposé à hauteur de hanche (propulsion de course).",
            "4. Redescends doucement avec la jambe libre en absorbant le choc."
        ),
        commonMistakes = "Ne pousse pas avec le mollet du pied resté au sol : c'est la jambe sur le cube qui doit tout faire.",
        drawableResName = "ex_step_up",
        videoResName = "video_step_up"
    )

    val exerciseBulgarianSquat = Exercise(
        id = "cube_bulgarian_squat",
        name = "Squats Bulgares Pied sur Cube",
        equipment = EquipmentType.PLYO_BOX,
        targetMuscle = "Quadriceps, Fessiers & Stabilité",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "PAR JAMBE",
        restSeconds = 20,
        voiceCue = "Pied arrière posé sur le cube, descends le genou arrière vers le sol à la verticale.",
        postureTip = "Le genou avant reste bien à 90° au-dessus de la cheville, pousse dans le talon avant.",
        animationType = AnimationType.PLYO_BOX_BULGARIAN_SQUAT,
        executionSteps = listOf(
            "1. PLACEMENT : Debout dos au cube en bois, pose le dessus du pied arrière (lacets à plat) sur l'arête du cube.",
            "2. ESPACEMENT : Avance le pied avant d'environ 90 cm à 1 m pour avoir un espacement suffisant.",
            "3. DESCENTE : Fléchis la jambe avant à la verticale. La cuisse avant arrive parallèle au sol (angle net de 90°). Le genou arrière descend droit vers le plancher.",
            "4. ALIGNEMENT : Le genou avant doit rester strictement au-dessus de la cheville avant, sans partir vers l'intérieur.",
            "5. REMONTÉE : Pousse fort et franchement dans le talon avant pour remonter sans bloquer brutalement le genou."
        ),
        commonMistakes = "Ne mets pas trop de poids sur le pied arrière (il ne sert que d'appui d'équilibre). 85% de l'effort se fait sur la jambe avant.",
        drawableResName = "ex_bulgarian_squat",
        videoResName = "video_bulgarian_squat"
    )

    // 7. POIDS DU CORPS (BURPEES, POMPES, SQUATS & GAINAGE FIN DE SÉANCE)
    val exerciseBurpee = Exercise(
        id = "bodyweight_burpee",
        name = "Burpees Complets Triathlète",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Cardio Explosif, Pectoraux & Cuisses",
        targetType = TargetType.REPETITIONS,
        targetCount = 10,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 25,
        voiceCue = "Départ debout, pose les mains au sol, jette les pieds en arrière, descends en pompe, ramène les pieds et saute bras en l'air !",
        postureTip = "Contrôle la descente, amortis la réception sur l'avant du pied, garde le tronc gainé sans cambrer.",
        animationType = AnimationType.BODYWEIGHT_BURPEE,
        executionSteps = listOf(
            "1. Position debout, pieds largeur des épaules.",
            "2. Fléchis les genoux, pose les mains au sol devant tes pieds et propulse les pieds en arrière en planche.",
            "3. Exécute une pompe complète en amenant la poitrine au sol.",
            "4. Ramène les pieds sous le bassin d'un bond explosif et saute verticalement bras tendus vers le haut."
        ),
        commonMistakes = "Ne laisse pas le bassin s'affaisser lors de la pompe ou de la transition en planche.",
        drawableResName = "ex_burpee",
        videoResName = "video_burpee"
    )

    val exercisePushups = Exercise(
        id = "bodyweight_pushups",
        name = "Pompes Classiques au Sol",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Pectoraux, Triceps & Poussée Natation",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Mains écartées largeur des épaules, corps rectiligne, descends la poitrine à 2 cm du sol et repousse fort.",
        postureTip = "Garde les coudes à 45 degrés du corps et le regard orienté légèrement vers l'avant.",
        animationType = AnimationType.BODYWEIGHT_PUSHUP,
        executionSteps = listOf(
            "1. Mains au sol écartées à la largeur des épaules, doigts légèrement ouverts.",
            "2. Corps droit des épaules aux talons, fessiers et abdos verrouillés.",
            "3. Fléchis les coudes en contrôlant la descente jusqu'à frôler le sol.",
            "4. Repousse le sol énergiquement jusqu'à l'extension complète des bras."
        ),
        commonMistakes = "Ne laisse pas les fesses monter en flèche ni le bas du dos se creuser.",
        drawableResName = "ex_pushup",
        videoResName = "video_pushup"
    )

    val exerciseAirSquat = Exercise(
        id = "bodyweight_air_squat",
        name = "Squats au Poids du Corps",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Quadriceps, Fessiers & Chaîne de Course",
        targetType = TargetType.REPETITIONS,
        targetCount = 15,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Pieds largeur des épaules, descends les fesses vers l'arrière et le bas sous la parallèle, buste fier.",
        postureTip = "Garde les talons cloués au sol, genoux orientés dans l'axe des pointes de pieds.",
        animationType = AnimationType.BODYWEIGHT_SQUAT,
        executionSteps = listOf(
            "1. Debout, pieds écartés largeur des épaules, pointes légèrement ouvertes vers l'extérieur (15°).",
            "2. Initie la flexion par les hanches vers l'arrière en descendant jusqu'à casser la parallèle.",
            "3. Garde le buste fier et les bras tendus devant pour équilibrer le centre de gravité.",
            "4. Pousse à travers tout le pied (notamment les talons) pour revenir debout."
        ),
        commonMistakes = "Ne décolle pas les talons et ne rentre pas les genoux vers l'intérieur.",
        drawableResName = "ex_air_squat",
        videoResName = "video_air_squat"
    )

    val exerciseGroundPlank = Exercise(
        id = "bodyweight_ground_plank",
        name = "Gainage Ventral au Sol (Planche)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Sangle Abdominale, Transverse & Alignement Nageur",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 45,
        repUnitLabel = "SECONDES",
        restSeconds = 20,
        voiceCue = "En appui sur les avant-bras et la pointe des pieds, maintiens une planche droite et immobile. Respire avec le ventre.",
        postureTip = "Rentre le nombril vers la colonne, verrouille les fessiers, aligne tête-colonne-talons.",
        animationType = AnimationType.BODYWEIGHT_PLANK,
        executionSteps = listOf(
            "1. Place les coudes au sol directement sous les articulations des épaules, avant-bras parallèles.",
            "2. Tends les jambes en appui sur la pointe des pieds, écartés de la largeur du bassin.",
            "3. Rétroverse légèrement le bassin en serrant vigoureusement les fessiers et les abdominaux profonds.",
            "4. Maintiens une ligne droite parfaite sans bouger jusqu'au coup de sifflet final."
        ),
        commonMistakes = "Ne monte pas le bassin en toit et ne laisse jamais le bas du dos cambrer.",
        drawableResName = "ex_plank",
        videoResName = "video_plank"
    )

    // 8. EXERCICES COMPLÉMENTAIRES DU POSTER "BODYWEIGHT WORKOUTS"
    val exerciseMountainClimbers = Exercise(
        id = "bodyweight_mountain_climber",
        name = "Mountain Climbers (Grimpeur au Sol)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Cardio Triathlon, Épaules & Gainage Dynamique",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 30,
        repUnitLabel = "SECONDES",
        restSeconds = 20,
        voiceCue = "En planche bras tendus, ramène alternativement les genoux vers la poitrine à cadence soutenue !",
        postureTip = "Garde les épaules au-dessus des mains, bassin bas sans rebondir.",
        animationType = AnimationType.BODYWEIGHT_MOUNTAIN_CLIMBER,
        executionSteps = listOf(
            "1. Démarre en position de planche haute, mains à plat sous les épaules, corps bien aligné.",
            "2. Amène le genou droit vers la poitrine d'un mouvement dynamique sans toucher le sol.",
            "3. Reviens en arrière tout en propulsant immédiatement le genou gauche vers l'avant.",
            "4. Adopte un rythme continu et régulier comme une course au sol."
        ),
        commonMistakes = "Évite de lever les fesses trop haut vers le ciel.",
        drawableResName = "ex_mountain_climber",
        videoResName = "video_mountain_climber"
    )

    val exerciseSwimmer = Exercise(
        id = "bodyweight_swimmer",
        name = "Le Nageur au Sol (Swimmer)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Grand Dorsal, Deltoïdes Postérieurs & Propulsion Natation",
        targetType = TargetType.TIME_SECONDS,
        targetCount = 40,
        repUnitLabel = "SECONDES",
        restSeconds = 20,
        voiceCue = "Allongé sur le ventre, soulève bras et jambes et bats alternativement comme dans l'eau en natation crawl.",
        postureTip = "Nuque longue, regarde le tapis, tire le mouvement depuis les hanches et les épaules.",
        animationType = AnimationType.BODYWEIGHT_SWIMMER,
        executionSteps = listOf(
            "1. Allonge-toi à plat ventre, bras tendus devant et jambes tendues derrière.",
            "2. Décolle simultanément les bras, la poitrine et les cuisses du sol.",
            "3. Soulève le bras droit et la jambe gauche, puis alterne avec le bras gauche et la jambe droite.",
            "4. Garde des battements synchronisés et rapides simulant la nage."
        ),
        commonMistakes = "Ne casse pas la nuque vers l'arrière, maintiens le regard orienté vers le sol.",
        drawableResName = "ex_swimmer",
        videoResName = "video_swimmer"
    )

    val exerciseLunges = Exercise(
        id = "bodyweight_lunges",
        name = "Fentes Avant Alternées (Lunges)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Quadriceps, Fessiers & Stabilité de Course",
        targetType = TargetType.REPETITIONS,
        targetCount = 16,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Fais un grand pas vers l'avant, descends le genou arrière vers le sol, buste droit, et repousse fort.",
        postureTip = "Le genou avant reste à 90° au-dessus de la cheville, pousse dans le talon.",
        animationType = AnimationType.BODYWEIGHT_LUNGES,
        executionSteps = listOf(
            "1. Debout, mains sur les hanches, pieds écartés de la largeur du bassin.",
            "2. Fais un grand pas vers l'avant et fléchis les deux genoux jusqu'à 90 degrés.",
            "3. Le genou arrière frôle le sol sans s'y écraser.",
            "4. Repousse vivement sur le talon avant pour revenir debout et change de jambe."
        ),
        commonMistakes = "Ne laisse pas le genou avant dépasser les orteils.",
        drawableResName = "ex_lunges",
        videoResName = "video_lunges"
    )

    val exerciseSuperman = Exercise(
        id = "bodyweight_superman",
        name = "Superman au Sol (Chaîne Postérieure)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Érecteurs du Rachis, Lombaires & Fessiers",
        targetType = TargetType.REPETITIONS,
        targetCount = 12,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "À plat ventre, décolle bras et jambes tendus du sol, bloque 2 secondes en haut puis redescends.",
        postureTip = "Contracte fort les fessiers et les lombaires, respire calmement.",
        animationType = AnimationType.BODYWEIGHT_SUPERMAN,
        executionSteps = listOf(
            "1. Couché à plat ventre sur le tapis, bras allongés devant soi.",
            "2. Soulève simultanément les bras, la poitrine et les jambes à 15 cm du sol.",
            "3. Maintiens la tension isométrique 2 secondes au sommet.",
            "4. Redescends doucement avec contrôle sans claquer."
        ),
        commonMistakes = "Ne donne pas d'à-coup brusque dans le bas du dos.",
        drawableResName = "ex_swimmer",
        videoResName = "video_swimmer"
    )

    val exerciseGluteBridge = Exercise(
        id = "bodyweight_glute_bridge",
        name = "Pont Fessier au Sol (Glute Bridge)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Grand Fessier, Ischio-jambiers & Extension Bassin",
        targetType = TargetType.REPETITIONS,
        targetCount = 15,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Allongé sur le dos, pieds à plat, pousse dans les talons pour monter le bassin droit.",
        postureTip = "Verrouille les fessiers une seconde en haut, aligne épaules, hanches et genoux.",
        animationType = AnimationType.BODYWEIGHT_GLUTE_BRIDGE,
        executionSteps = listOf(
            "1. Allonge-toi sur le dos, genoux pliés et pieds à plat au sol écartés de la largeur des hanches.",
            "2. Enfonce fermement les talons dans le sol et lève les hanches jusqu'à l'alignement genoux-bassin-épaules.",
            "3. Presse vigoureusement les fessiers au sommet.",
            "4. Redescends doucement en frôlant le sol avant la répétition suivante."
        ),
        commonMistakes = "Ne cambre pas le haut du dos, l'extension doit venir des hanches.",
        drawableResName = "ex_air_squat",
        videoResName = "video_air_squat"
    )

    val exerciseBicycleCrunches = Exercise(
        id = "bodyweight_bicycle_crunches",
        name = "Bicycle Crunches (Pédalage Abdos)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Obliques & Coordination Pédalage Cyclisme",
        targetType = TargetType.REPETITIONS,
        targetCount = 20,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "Mains aux tempes, touche le genou avec le coude opposé en allongeant l'autre jambe.",
        postureTip = "Pivote avec les épaules, ne tire pas sur la nuque, cadence contrôlée.",
        animationType = AnimationType.BODYWEIGHT_BICYCLE_CRUNCH,
        executionSteps = listOf(
            "1. Sur le dos, mains derrière les oreilles, jambes décollées du sol à 90°.",
            "2. Enroule le haut du dos et amène le coude droit vers le genou gauche tout en tendant la jambe droite.",
            "3. Inverse le mouvement avec fluidité sans reposer le haut du dos.",
            "4. Respire de manière régulière à chaque alternance."
        ),
        commonMistakes = "Évite les mouvements de coudes seuls : c'est la cage thoracique entière qui tourne.",
        drawableResName = "ex_ab_crunch",
        videoResName = "video_ab_crunch"
    )

    val exerciseCalfRaises = Exercise(
        id = "bodyweight_calf_raises",
        name = "Élévations Mollets au Sol (Calf Raises)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Triceps Sural, Tendon d'Achille & Foulée",
        targetType = TargetType.REPETITIONS,
        targetCount = 20,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 15,
        voiceCue = "Debout pieds parallèles, monte au maximum sur la pointe des pieds et contrôle la redescente.",
        postureTip = "Renforce les tendons d'Achille pour encaisser les kilomètres en course à pied.",
        animationType = AnimationType.BODYWEIGHT_CALF_RAISE,
        executionSteps = listOf(
            "1. Debout, pieds écartés de la largeur du bassin, buste droit.",
            "2. Soulève les talons en poussant fermement sur la pulpe des orteils pour monter à l'extension maximale.",
            "3. Marque 1 seconde d'arrêt en haut en contractant les mollets.",
            "4. Redescends lentement sans laisser les talons s'écraser au sol."
        ),
        commonMistakes = "Ne te balance pas d'avant en arrière, monte verticalement.",
        drawableResName = "ex_step_up",
        videoResName = "video_step_up"
    )

    val exerciseRussianTwist = Exercise(
        id = "bodyweight_russian_twist",
        name = "Russian Twist (Rotations Russes)",
        equipment = EquipmentType.BODYWEIGHT,
        targetMuscle = "Obliques, Ceinture Abdominale & Stabilité Buste",
        targetType = TargetType.REPETITIONS,
        targetCount = 20,
        repUnitLabel = "RÉPÉTITIONS",
        restSeconds = 20,
        voiceCue = "En équilibre fessier, buste incliné en arrière, fais pivoter les mains de chaque côté du bassin.",
        postureTip = "Dos plat, abdos serrés, accompagne le mouvement avec les yeux.",
        animationType = AnimationType.BODYWEIGHT_RUSSIAN_TWIST,
        executionSteps = listOf(
            "1. Assis au sol, genoux pliés, pieds légèrement décollés du sol (ou posés pour commencer).",
            "2. Incline le tronc en arrière à 45° en maintenant la colonne droite.",
            "3. Joins les mains et pivote les épaules vers la droite pour toucher le sol, puis immédiatement vers la gauche.",
            "4. Contrôle la vitesse sans à-coups."
        ),
        commonMistakes = "Ne courbe pas le dos en rond : conserve le dos droit et la poitrine fière.",
        drawableResName = "ex_ab_crunch",
        videoResName = "video_ab_crunch"
    )

    fun getDedicatedPosterSession(): WorkoutSession {
        return WorkoutSession(
            id = "session_bodyweight_poster",
            dayOfWeek = 0,
            dayName = "Poster Poids de Corps",
            splitType = SplitType.SPLIT_BODYWEIGHT,
            estimatedDurationMinutes = 40,
            exercises = listOf(
                exerciseAirSquat,          // Lower Body
                exercisePushups,           // Upper Body
                exerciseLunges,            // Lower Body
                exerciseSwimmer,           // Back / Natation
                exerciseMountainClimbers,  // Total Body / Cardio
                exerciseBicycleCrunches,   // Core
                exerciseGluteBridge,       // Lower Body
                exerciseBurpee,            // Total Body
                exerciseSuperman,          // Back
                exerciseRussianTwist,      // Core
                exerciseCalfRaises,        // Lower Body
                exerciseGroundPlank        // Core Finisher
            )
        )
    }

    fun getWeeklySplitSessions(): List<WorkoutSession> {
        return listOf(
            WorkoutSession(
                id = "session_mon",
                dayOfWeek = 1,
                dayName = "Lundi",
                splitType = SplitType.SPLIT_A,
                estimatedDurationMinutes = 38,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exerciseLunges,
                    exerciseAirSquat,
                    exerciseBulgarianSquat,
                    exerciseStepUp,
                    exerciseAbCrunch,
                    exerciseCalfRaises,
                    exerciseGroundPlank // Finisher Gainage
                )
            ),
            WorkoutSession(
                id = "session_tue",
                dayOfWeek = 2,
                dayName = "Mardi",
                splitType = SplitType.SPLIT_B,
                estimatedDurationMinutes = 38,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exercisePushups,
                    exerciseInvertedRow,
                    exerciseParallelDips,
                    exerciseSwimmer,
                    exerciseHyperextension,
                    exerciseParallelKneeRaises,
                    exerciseGroundPlank // Finisher Gainage
                )
            ),
            WorkoutSession(
                id = "session_wed",
                dayOfWeek = 3,
                dayName = "Mercredi",
                splitType = SplitType.SPLIT_C,
                estimatedDurationMinutes = 38,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exerciseBurpee,
                    exerciseMountainClimbers,
                    exerciseBalanceSquat,
                    exercisePushups,
                    exerciseBicycleCrunches,
                    exerciseLunges,
                    exerciseGroundPlank // Finisher Gainage
                )
            ),
            WorkoutSession(
                id = "session_thu",
                dayOfWeek = 4,
                dayName = "Jeudi",
                splitType = SplitType.SPLIT_A,
                estimatedDurationMinutes = 38,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exerciseGluteBridge,
                    exerciseBulgarianSquat,
                    exerciseAirSquat,
                    exerciseStepUp,
                    exerciseRussianTwist,
                    exerciseCalfRaises,
                    exerciseGroundPlank // Finisher Gainage
                )
            ),
            WorkoutSession(
                id = "session_fri",
                dayOfWeek = 5,
                dayName = "Vendredi",
                splitType = SplitType.SPLIT_B,
                estimatedDurationMinutes = 38,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exerciseSwimmer,
                    exercisePushups,
                    exerciseInvertedRow,
                    exerciseParallelDips,
                    exerciseSuperman,
                    exerciseParallelKneeRaises,
                    exerciseGroundPlank // Finisher Gainage
                )
            ),
            WorkoutSession(
                id = "session_sat",
                dayOfWeek = 6,
                dayName = "Samedi",
                splitType = SplitType.SPLIT_D,
                estimatedDurationMinutes = 40,
                exercises = listOf(
                    exerciseEllipticalWarmup,
                    exerciseBurpee,
                    exerciseMountainClimbers,
                    exerciseLunges,
                    exerciseSwimmer,
                    exercisePushups,
                    exerciseBalancePlank,
                    exerciseGroundPlank // Finisher Gainage 60s
                )
            ),
            WorkoutSession(
                id = "session_sun",
                dayOfWeek = 7,
                dayName = "Dimanche",
                splitType = SplitType.SPLIT_A,
                isRestDay = true,
                estimatedDurationMinutes = 0,
                exercises = emptyList()
            )
        )
    }
}
