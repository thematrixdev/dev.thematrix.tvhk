package dev.thematrix.tvhk

import java.io.Serializable

data class Movie(
    var id: Int = 0,
    var categoryId: Int = -1,
    var title: String = "",
    var description: String = "",
    var cardImageUrl: String = "",
    var videoUrl: String = "",
    var func: String = "",
    var hongkongonly: Boolean = false
) : Serializable {
    companion object {
        internal const val serialVersionUID = 727566175075960653L
    }
}
