package br.com.tworemember.localizer

import br.com.tworemember.localizer.model.User
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.FirebaseDatabase

class UserDAO {
    private val db = FirebaseDatabase.getInstance()

    fun updateDb(firebaseUser: FirebaseUser): User?{
        val users = db.reference.child("users")
        firebaseUser.email?.let {
            val key = it.toBase64()
            val user = User(key, firebaseUser.displayName, it)
            users.child(key).setValue(user)

            return user
        }
        return null
    }
}