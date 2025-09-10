package com.example.dip.ui.history

import android.view.View


fun View.animateClick() {
    this.animate()
        .scaleX(0.95f)
        .scaleY(0.95f)
        .setDuration(80)
        .withEndAction {
            this.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
        }.start()
}