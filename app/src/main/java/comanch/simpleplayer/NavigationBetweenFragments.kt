package comanch.simpleplayer

import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment.Companion.findNavController
import javax.inject.Inject

class NavigationBetweenFragments @Inject constructor() {

    fun navigateToDestination(fragment: Fragment, destination: NavDirections)
            = with(findNavController(fragment)) {
        currentDestination?.getAction(destination.actionId)
            ?.let { navigate(destination) }
    }
}