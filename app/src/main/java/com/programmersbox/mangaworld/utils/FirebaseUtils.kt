package com.programmersbox.mangaworld.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Source
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDao
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_db.MangaReadChapter
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.R
import com.programmersbox.rxutils.toLatestFlowable
import io.reactivex.Flowable
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class FirebaseAuthentication(private val context: Context, private val activity: Activity) {

    private val RC_SIGN_IN = 32

    private var gso: GoogleSignInOptions? = null

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private var googleSignInClient: GoogleSignInClient? = null

    fun authenticate() {
        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso!!)
    }

    fun signIn() {
        //val signInIntent = googleSignInClient!!.signInIntent
        //activity.startActivityForResult(signInIntent, RC_SIGN_IN)
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        // Create and launch sign-in intent
        activity.startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setTheme(R.style.AppTheme)
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }

    fun signOut() {
        auth.signOut()
        //currentUser = null
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                //currentUser = auth.currentUser//FirebaseAuth.getInstance().currentUser
                Loged.f(currentUser)
                // ...
                //val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                //val account = task.getResult(ApiException::class.java)!!
                //Loged.d("firebaseAuthWithGoogle:" + account.id)
                //googleAccount = account
                //firebaseAuthWithGoogle(account.idToken!!)
                Toast.makeText(context, "Signed in Successfully", Toast.LENGTH_SHORT).show()
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
                Toast.makeText(context, "Signed in Unsuccessfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener(activity) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                //Log.d(TAG, "signInWithCredential:success")
                //val user = auth.currentUser
                //updateUI(user)
                //currentUser = auth.currentUser
                Toast.makeText(context, "Signed in Successfully", Toast.LENGTH_SHORT).show()
            } else {
                // If sign in fails, display a message to the user.
                //Log.w(TAG, "signInWithCredential:failure", task.exception)
                // ...
                //Snackbar.make(view, "Authentication Failed.", Snackbar.LENGTH_SHORT).show()
                //updateUI(null)
                Toast.makeText(context, "Signed in Unsuccessfully", Toast.LENGTH_SHORT).show()
            }

            // ...
        }
    }

    fun onStart() {
        //currentUser = auth.currentUser
    }

    companion object {
        //var googleAccount: GoogleSignInAccount? = null
        //private set
        val currentUser: FirebaseUser? get() = FirebaseAuth.getInstance().currentUser
        //private set
    }

}

object FirebaseDb {

    private const val DOCUMENT_ID = "favoriteManga"
    private const val CHAPTERS_ID = "chaptersRead"

    private val db = FirebaseFirestore.getInstance().apply {
        firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
    }

    private fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

    suspend fun uploadAllItems(dao: MangaDao) {
        val manga = dao.getAllMangaSync().map { it.toMangaModel() }

        val chapters = dao.getAllChapters()

        val store = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(DOCUMENT_ID)
                .set(DOCUMENT_ID to manga)
        }

        store?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }

        val store1 = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(CHAPTERS_ID)
                .set(CHAPTERS_ID to chapters)
        }

        store1?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }
    }

    suspend fun addManga(mangaModel: MangaModel) {
        val store = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(DOCUMENT_ID)
                .update("second", FieldValue.arrayUnion(mangaModel.toFirebaseManga()))
        }

        store?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }
    }

    suspend fun removeManga(mangaModel: MangaModel) {
        val store = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(DOCUMENT_ID)
                .update("second", FieldValue.arrayRemove(mangaModel.toFirebaseManga()))
        }

        store?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }
    }

    private fun MangaModel.toFirebaseManga() = FirebaseManga(title, description, mangaUrl, imageUrl, source)
    private fun FirebaseManga.toMangaModel() = MangaModel(title!!, description!!, mangaUrl!!, imageUrl!!, source!!)

    private data class FirebaseManga(
        val title: String? = null,
        val description: String? = null,
        val mangaUrl: String? = null,
        val imageUrl: String? = null,
        val source: Sources? = null
    )

    private data class FirebaseAllManga(val first: String = DOCUMENT_ID, val second: List<FirebaseManga> = emptyList())

    suspend fun getAllManga() = db
        .collection(FirebaseAuthentication.currentUser?.uid!!)
        .document(DOCUMENT_ID)
        .get(Source.DEFAULT)
        .await()
        .toObject(FirebaseAllManga::class.java)
        ?.second
        ?.map { it.toMangaModel() }

    fun getMangaByUrl(url: String): Flowable<MangaDbModel> = PublishSubject.create<MangaDbModel> { emitter ->
        db
            .collection(FirebaseAuthentication.currentUser?.uid!!)
            .document(DOCUMENT_ID)
            .addSnapshotListener { documentSnapshot, _ ->
                documentSnapshot?.toObject(FirebaseAllManga::class.java)
                    ?.second
                    ?.map { it.toMangaModel() }
                    ?.find { it.mangaUrl == url }
                    .let { it?.let { emitter.onNext(it.toMangaDbModel()) } ?: emitter.onError(Throwable("Not in here")) }
            }
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    fun findMangaByUrl(url: String): Flowable<Boolean> = PublishSubject.create<Boolean> { emitter ->
        db
            .collection(FirebaseAuthentication.currentUser?.uid!!)
            .document(DOCUMENT_ID)
            .addSnapshotListener { documentSnapshot, _ ->
                documentSnapshot?.toObject(FirebaseAllManga::class.java)
                    ?.second
                    ?.map { it.toMangaModel() }
                    ?.find { it.mangaUrl == url }
                    .let { emitter.onNext(it != null) }
            }
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    fun getAllMangaFlowable(): Flowable<List<MangaModel>> = PublishSubject.create<List<MangaModel>> { emitter ->
        db
            .collection(FirebaseAuthentication.currentUser?.uid!!)
            .document(DOCUMENT_ID)
            .addSnapshotListener { documentSnapshot, _ ->
                documentSnapshot?.toObject(FirebaseAllManga::class.java)?.second?.map { it.toMangaModel() }?.let { emitter.onNext(it) }
            }
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    private data class FirebaseChapter(
        val url: String? = null,
        val name: String? = null,
        val mangaUrl: String? = null
    )

    private data class FirebaseAllChapter(val first: String = CHAPTERS_ID, val second: List<FirebaseChapter> = emptyList())

    private fun MangaReadChapter.toFirebaseChapter() = FirebaseChapter(url, name, mangaUrl)
    private fun FirebaseChapter.toMangaChapter() = MangaReadChapter(url!!, name!!, mangaUrl!!)

    suspend fun getAllChapters() = db
        .collection(FirebaseAuthentication.currentUser?.uid!!)
        .document(CHAPTERS_ID)
        .get(Source.DEFAULT)
        .await()
        .toObject(FirebaseAllChapter::class.java)
        ?.second
        ?.map { it.toMangaChapter() }

    fun getAllChapterFlowable(): Flowable<List<MangaReadChapter>> = PublishSubject.create<List<MangaReadChapter>> { emitter ->
        db
            .collection(FirebaseAuthentication.currentUser?.uid!!)
            .document(CHAPTERS_ID)
            .addSnapshotListener { documentSnapshot, _ ->
                documentSnapshot?.toObject(FirebaseAllChapter::class.java)
                    ?.second
                    ?.map { it.toMangaChapter() }?.let { emitter.onNext(it) }
            }
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    suspend fun addChapter(mangaModel: MangaReadChapter) {
        val store = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(CHAPTERS_ID)
                .update("second", FieldValue.arrayUnion(mangaModel.toFirebaseChapter()))
        }

        store?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }
    }

    suspend fun removeChapter(mangaModel: MangaReadChapter) {
        val store = FirebaseAuthentication.currentUser?.let {
            db.collection(it.uid)
                .document(CHAPTERS_ID)
                .update("second", FieldValue.arrayRemove(mangaModel.toFirebaseChapter()))
        }

        store?.addOnSuccessListener {
            Loged.d("Success!")
        }?.addOnFailureListener {
            Loged.wtf("Failure!")
        }?.addOnCompleteListener {
            Loged.d("All done!")
        }
    }

}

fun Context.dbAndFireManga(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = Flowables.combineLatest(
    dao.getAllManga(),
    FirebaseDb.getAllMangaFlowable()
) { db, fire -> (db.map { it.toMangaModel() } + (fire)).distinctBy { it.mangaUrl } }

fun Context.dbAndFireChapter(
    url: String,
    dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()
) = Flowables.combineLatest(
    dao.getReadChaptersById(url),
    FirebaseDb.getAllChapterFlowable()
) { db, fire -> (db + fire).distinctBy { it.url } }