package com.littlebug.kdi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel

class LifecycleAwareDI(private val dependencyGraph: DependencyGraph) {

    fun <T : Any> inject(activity: AppCompatActivity): T {
        val instance = dependencyGraph.resolve(activity::class, activity)
        dependencyGraph.inject(activity, activity)
        activity.lifecycle.addObserver(ActivityScopeObserver(activity))
        return instance
    }

    fun <T : Any> inject(fragment: Fragment): T {
        val instance = dependencyGraph.resolve(fragment::class, fragment)
        dependencyGraph.inject(fragment, fragment)
        fragment.lifecycle.addObserver(FragmentScopeObserver(fragment))
        return instance
    }

    fun <T : Any> inject(viewModel: ViewModel): T {
        val instance = dependencyGraph.resolve(viewModel::class, viewModel)
        dependencyGraph.inject(viewModel, viewModel)
        return instance
    }

    private class ActivityScopeObserver(private val activity: AppCompatActivity) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dependencyGraph.clearActivityScope(activity)
        }
    }

    private class FragmentScopeObserver(private val fragment: Fragment) : DefaultLifecycleObserver {
        override fun onDestroy(owner: LifecycleOwner) {
            dependencyGraph.clearFragmentScope(fragment)
        }
    }
}