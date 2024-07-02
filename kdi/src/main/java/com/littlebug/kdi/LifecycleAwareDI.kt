package com.littlebug.kdi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

class LifecycleAwareDI(val dependencyGraph: DependencyGraph) {

    internal inline fun <reified T : Any> inject(activity: AppCompatActivity): T {
        val instance = dependencyGraph.resolve(T::class, activity)
        dependencyGraph.inject(activity, activity)
        activity.lifecycle.addObserver(ActivityScopeObserver(activity))
        return instance
    }

    internal inline fun <reified T : Any> inject(fragment: Fragment): T {
        val instance = dependencyGraph.resolve(T::class, fragment)
        dependencyGraph.inject(fragment, fragment)
        fragment.lifecycle.addObserver(FragmentScopeObserver(fragment))
        return instance
    }

    inline fun <reified T : Any> inject(viewModel: ViewModel): T {
        val instance = dependencyGraph.resolve(T::class, viewModel)
        dependencyGraph.inject(viewModel, viewModel)
        return instance
    }

    internal inner class ActivityScopeObserver(
        private val activity: AppCompatActivity
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dependencyGraph.clearActivityScope(activity)
        }
    }

    internal inner class FragmentScopeObserver(
        private val fragment: Fragment
    ) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dependencyGraph.clearFragmentScope(fragment)
        }
    }
}