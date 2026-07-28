package com.shootcraft.plugin.game;

/**
 * Etats possibles d'une arene ShootCraft.
 *
 *   NOT_CONFIGURED - l'arene n'a pas encore de lobby / spawns valides
 *   WAITING        - en attente de joueurs dans le lobby (pas de compte a rebours actif)
 *   COUNTDOWN      - compte a rebours du lobby en cours (minimum de joueurs atteint)
 *   PLAYING        - partie en cours (les joueurs peuvent encore la rejoindre)
 *   ENDING         - partie terminee, ecran de score + proposition "Rejouer/Quitter"
 */
public enum GameState {
    NOT_CONFIGURED,
    WAITING,
    COUNTDOWN,
    PLAYING,
    ENDING
}
