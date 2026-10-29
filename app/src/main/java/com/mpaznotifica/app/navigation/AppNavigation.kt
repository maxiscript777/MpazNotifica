package com.mpaznotifica.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mpaznotifica.app.ui.screen.LoginScreen
import com.mpaznotifica.app.ui.screen.PanelScreen
import com.mpaznotifica.app.ui.screen.apoderados.ApoderadosScreen
import com.mpaznotifica.app.ui.screen.comunicados.DetalleComunicadoScreen
import com.mpaznotifica.app.ui.screen.comunicados.HistorialScreen
import com.mpaznotifica.app.ui.screen.comunicados.RedactarComunicadoScreen
import com.mpaznotifica.app.viewmodel.MpazNotificaViewModel

object Rutas {
    const val LOGIN = "login"
    const val PANEL = "panel"
    const val REDACTAR = "redactar"
    const val HISTORIAL = "historial"
    const val DETALLE = "detalle"
    const val APODERADOS = "apoderados"
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: MpazNotificaViewModel = viewModel()
) {
    // cierra sesion y limpia la pila
    val cerrarSesion: () -> Unit = {
        viewModel.logout()
        navController.navigate(Rutas.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    // rutas y animaciones de pantalla
    NavHost(
        navController = navController,
        startDestination = Rutas.LOGIN,
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeIn(tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300)) + fadeOut(tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeIn(tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300)) + fadeOut(tween(300))
        }
    ) {
        composable(Rutas.LOGIN) {
            LoginScreen(
                viewModel = viewModel,
                onIngresar = {
                    navController.navigate(Rutas.PANEL) {
                        popUpTo(Rutas.LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(Rutas.PANEL) {
            PanelScreen(navController, viewModel, onCerrarSesion = cerrarSesion)
        }
        composable(Rutas.REDACTAR) {
            RedactarComunicadoScreen(navController, viewModel)
        }
        composable(Rutas.HISTORIAL) {
            HistorialScreen(navController, viewModel)
        }
        composable(Rutas.DETALLE) {
            DetalleComunicadoScreen(navController, viewModel)
        }
        composable(Rutas.APODERADOS) {
            ApoderadosScreen(navController, viewModel)
        }
    }
}
