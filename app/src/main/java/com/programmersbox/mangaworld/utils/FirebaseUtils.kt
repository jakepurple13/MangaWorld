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
import com.google.firebase.firestore.*
import com.google.firebase.firestore.ktx.toObjects
import com.programmersbox.helpfulutils.runOnUIThread
import com.programmersbox.loggingutils.Loged
import com.programmersbox.loggingutils.f
import com.programmersbox.manga_db.MangaDao
import com.programmersbox.manga_db.MangaDatabase
import com.programmersbox.manga_db.MangaDbModel
import com.programmersbox.manga_db.MangaReadChapter
import com.programmersbox.manga_sources.mangasources.MangaModel
import com.programmersbox.manga_sources.mangasources.Sources
import com.programmersbox.mangaworld.R
import com.programmersbox.rxutils.invoke
import com.programmersbox.rxutils.toLatestFlowable
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.Flowables
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

class FirebaseAuthentication(private val context: Context, private val activity: Activity) {

    private val RC_SIGN_IN = 32

    private var gso: GoogleSignInOptions? = null

    val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
            //.setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            //.setCacheSizeBytes()
            .build()
    }

    private val mangaDoc get() = FirebaseAuthentication.currentUser?.let { db.collection(it.uid).document(DOCUMENT_ID) }
    private val chapterDoc get() = FirebaseAuthentication.currentUser?.let { db.collection(it.uid).document(CHAPTERS_ID) }

    private fun <TResult> Task<TResult>.await(): TResult = Tasks.await(this)

    private val mangaDoc2 get() = FirebaseAuthentication.currentUser?.let { db.collection("mangaworld").document(DOCUMENT_ID).collection(it.uid) }
    private val chapterDoc2 get() = FirebaseAuthentication.currentUser?.let { db.collection("mangaworld").document(CHAPTERS_ID).collection(it.uid) }

    suspend fun uploadAllItems2(dao: MangaDao, context: Context) {
        //Todo: make a workmanager request for this
        //throw Exception("Dont forget to get current firestore items")
        val m = listOfNotNull(getAllManga(), dao.getAllMangaSync()).flatten().map { it.toFirebaseManga().apply { chapterCount = it.numChapters } }
        m.forEachIndexed { index, firebaseManga ->
            firebaseManga.mangaUrl?.replace("/", "<")?.let { it1 -> mangaDoc2?.document(it1)?.set(firebaseManga) }
                ?.addOnSuccessListener {
                    if (index >= m.size) {
                        runOnUIThread {
                            Toast.makeText(context, "Finished Manga", Toast.LENGTH_LONG).show()
                        }
                    }
                    Loged.d("Success!")
                }?.addOnFailureListener {
                    Loged.wtf("Failure!")
                }?.addOnCompleteListener {
                    Loged.d("All done!")
                }
        }

        /*val c = listOfNotNull(getAllChapters(), dao.getAllChapters()).flatten().distinctBy { it.url }.map { it.toFirebaseChapter() }
        var cCount = 0
        c
            .forEach {
                it.url?.replace("/", "<")?.let { it1 -> chapterDoc2?.document(it1)?.set(it) }
                    ?.addOnSuccessListener {
                        cCount++
                        if (cCount >= m.size) {
                            runOnUIThread {
                                Toast.makeText(context, "Finished Chapters", Toast.LENGTH_LONG).show()
                            }
                        }
                        Loged.d("Success!")
                    }?.addOnFailureListener {
                        Loged.wtf("Failure!")
                    }?.addOnCompleteListener {
                        Loged.d("All done!")
                    }
            }*/

        /*chapterDoc2
            ?.set(CHAPTERS_ID to dao.getAllChapters().map { it.toFirebaseChapter() })
            ?.addOnSuccessListener {
                Loged.d("Success!")
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
            }?.addOnCompleteListener {
                Loged.d("All done!")
            }*/
    }

    suspend fun uploadAllItems(dao: MangaDao) {
        mangaDoc?.set(DOCUMENT_ID to dao.getAllMangaSync().map { it.toMangaModel().toFirebaseManga().apply { chapterCount = it.numChapters } })
            ?.addOnSuccessListener {
                Loged.d("Success!")
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
            }?.addOnCompleteListener {
                Loged.d("All done!")
            }

        chapterDoc
            ?.set(CHAPTERS_ID to dao.getAllChapters().map { it.toFirebaseChapter() })
            ?.addOnSuccessListener {
                Loged.d("Success!")
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
            }?.addOnCompleteListener {
                Loged.d("All done!")
            }
    }

    fun addManga(mangaModel: MangaModel) = Completable.create { emitter ->
        mangaDoc
            ?.update("second", FieldValue.arrayUnion(mangaModel.toFirebaseManga().apply { chapterCount = mangaModel.toInfoModel().chapters.size }))
            ?.addOnSuccessListener {
                Loged.d("Success!")
                emitter()
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

    fun addManga(mangaModel: MangaModel, chapterSize: Int) = Completable.create { emitter ->
        mangaDoc
            ?.update("second", FieldValue.arrayUnion(mangaModel.toFirebaseManga().apply { chapterCount = chapterSize }))
            ?.addOnSuccessListener {
                Loged.d("Success!")
                emitter()
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

    fun updateManga(mangaDbModel: MangaDbModel) = Completable.create { emitter ->
        mangaDoc
            ?.update("second", FieldValue.arrayUnion(mangaDbModel.toFirebaseManga()))
            ?.addOnSuccessListener {
                Loged.d("Success!")
                emitter()
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

    fun removeManga(mangaModel: MangaModel) = Completable.create { emitter ->
        mangaDoc
            ?.update("second", FieldValue.arrayRemove(mangaModel.toFirebaseManga()))
            ?.addOnSuccessListener {
                Loged.d("Success!")
                emitter()
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

    fun updateManga2(mangaDbModel: MangaDbModel) = Completable.create { emitter ->
        mangaDbModel.mangaUrl.replace("/", "<").let {
            mangaDoc2
                ?.document(it)
                ?.update("chapterCount", mangaDbModel.numChapters)
                ?.addOnSuccessListener {
                    Loged.d("Success!")
                    emitter()
                }?.addOnFailureListener {
                    Loged.wtf("Failure!")
                    emitter(it)
                }?.addOnCompleteListener {
                    Loged.d("All done!")
                }
        } ?: emitter()
    }

    fun addManga2(mangaModel: MangaModel, chapterSize: Int) = Completable.create { emitter ->
        mangaModel.mangaUrl.replace("/", "<").let {
            mangaDoc2
                ?.document(it)
                ?.set(mangaModel.toFirebaseManga().apply { chapterCount = chapterSize })
                ?.addOnSuccessListener {
                    Loged.d("Success!")
                    emitter()
                }?.addOnFailureListener {
                    Loged.wtf("Failure!")
                    emitter(it)
                }?.addOnCompleteListener {
                    Loged.d("All done!")
                }
        } ?: emitter()
    }

    fun removeManga2(mangaModel: MangaModel) = Completable.create { emitter ->
        mangaModel.toFirebaseManga().mangaUrl?.replace("/", "<")?.let {
            mangaDoc2
                ?.document(it)
                ?.delete()
                ?.addOnSuccessListener {
                    Loged.d("Success!")
                    emitter()
                }?.addOnFailureListener {
                    Loged.wtf("Failure!")
                    emitter(it)
                }?.addOnCompleteListener {
                    Loged.d("All done!")
                }
        } ?: emitter()
    }

    private fun MangaModel.toFirebaseManga() = FirebaseManga(title, description, mangaUrl, imageUrl, source)
    private fun FirebaseManga.toMangaModel() = MangaModel(title!!, description!!, mangaUrl!!, imageUrl!!, source!!)
    private fun MangaDbModel.toFirebaseManga() = FirebaseManga(title, description, mangaUrl, imageUrl, source, numChapters)
    private fun FirebaseManga.toMangaDbModel() = MangaDbModel(title!!, description!!, mangaUrl!!, imageUrl!!, source!!, chapterCount)

    private data class FirebaseManga(
        val title: String? = null,
        val description: String? = null,
        val mangaUrl: String? = null,
        val imageUrl: String? = null,
        val source: Sources? = null,
        var chapterCount: Int = 0
    )

    private data class FirebaseAllManga(val first: String = DOCUMENT_ID, val second: List<FirebaseManga> = emptyList())

    fun getAllManga() = mangaDoc
        ?.get(Source.DEFAULT)
        ?.await()
        ?.toObject(FirebaseAllManga::class.java)
        ?.second
        ?.map { it.toMangaDbModel() }

    fun getAllManga2() = mangaDoc2
        ?.get()
        ?.await()
        ?.toObjects<FirebaseManga>()
        ?.map { it.toMangaDbModel() }

    fun getMangaByUrl(url: String): Flowable<MangaDbModel> = PublishSubject.create<MangaDbModel> { emitter ->
        mangaDoc?.addSnapshotListener { documentSnapshot, _ ->
            documentSnapshot?.toObject(FirebaseAllManga::class.java)
                ?.second
                ?.map { it.toMangaModel() }
                ?.find { it.mangaUrl == url }
                .let { it?.let { emitter(it.toMangaDbModel()) } ?: emitter(Throwable("Not in here")) }
        }
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    fun findMangaByUrl(url: String): Flowable<Boolean> = PublishSubject.create<Boolean> { emitter ->
        mangaDoc?.addSnapshotListener { documentSnapshot, _ ->
            documentSnapshot?.toObject(FirebaseAllManga::class.java)
                ?.second
                ?.map { it.toMangaModel() }
                ?.find { it.mangaUrl == url }
                .also { println(it) }
                .let { emitter(it != null) }
        }
    }.toLatestFlowable()

    fun findMangaByUrlSingleTwo(url: String): Single<Boolean> = Single.create<Boolean> { emitter ->
        mangaDoc2
            ?.whereEqualTo("mangaUrl", url)
            ?.get()
            ?.addOnSuccessListener { emitter(it?.toObjects<FirebaseManga>()?.isNotEmpty()) }
            ?.addOnFailureListener { emitter(it) }
    }

    fun findMangaByUrlSingle(url: String): Single<Boolean> = Single.create { emitter ->
        mangaDoc
            ?.get(Source.DEFAULT)
            ?.await()
            ?.toObject(FirebaseAllManga::class.java)
            ?.second
            ?.map { it.toMangaModel() }
            ?.find { it.mangaUrl == url }
            .let { emitter(it != null) }
    }

    fun findMangaByUrlMaybe(url: String): Maybe<Boolean> = Maybe.create { emitter ->
        mangaDoc
            ?.get(Source.DEFAULT)
            ?.await()
            ?.toObject(FirebaseAllManga::class.java)
            ?.second
            ?.map { it.toMangaModel() }
            ?.find { it.mangaUrl == url }
            .let { emitter(it != null) }
        emitter()

        /*addSnapshotListener { documentSnapshot, _ ->
            documentSnapshot?.toObject(FirebaseAllManga::class.java)
                ?.second
                ?.map { it.toMangaModel() }
                ?.find { it.mangaUrl == url }
                .let {
                    emitter.onSuccess(it != null)
                    *//*if (it != null) emitter.onSuccess(it != null)
                    else emitter.onError(Throwable("Don't have it"))*//*
                }
        }*/
    }

    private var allMangaFlowableListener: ListenerRegistration? = null

    fun detachListener() {
        allMangaFlowableListener?.remove()
        allMangaFlowableListener = null
    }

    fun getAllMangaFlowable(): Flowable<List<MangaDbModel>> = PublishSubject.create<List<MangaDbModel>> { emitter ->
        allMangaFlowableListener?.remove()
        allMangaFlowableListener = mangaDoc?.addSnapshotListener { documentSnapshot, _ ->
            documentSnapshot?.toObject(FirebaseAllManga::class.java)?.second?.map { it.toMangaDbModel() }?.let { emitter(it) }
        }
        if (allMangaFlowableListener == null) emitter()
    }.subscribeOn(Schedulers.io()).toLatestFlowable()

    fun getAllMangaToFlowable(update: (List<MangaDbModel>) -> Unit) = mangaDoc?.addSnapshotListener { documentSnapshot, _ ->
        documentSnapshot?.toObject(FirebaseAllManga::class.java)?.second?.map { it.toMangaDbModel() }?.let { update(it) }
    }

    class FirebaseListener {

        var listener: ListenerRegistration? = null
            private set

        fun getAllMangaFlowable() = PublishSubject.create<List<MangaDbModel>> { emitter ->
            listener = mangaDoc2?.addSnapshotListener { value, error ->
                value?.toObjects<FirebaseManga>()?.map { it.toMangaDbModel() }?.let { emitter(it) }
            }
            if (listener == null) emitter()
        }.toLatestFlowable()

        fun getAllManga() = mangaDoc2
            ?.get()
            ?.await()
            ?.toObjects<FirebaseManga>()
            ?.map { it.toMangaDbModel() }

    }

    fun getAllMangaFlowable2() = mangaDoc2
        ?.get()
        ?.await()
        ?.toObjects<FirebaseManga>()
        ?.map { it.toMangaDbModel() }

    private data class FirebaseChapter(
        val url: String? = null,
        val name: String? = null,
        val mangaUrl: String? = null
    )

    private data class FirebaseAllChapter(val first: String = CHAPTERS_ID, val second: List<FirebaseChapter> = emptyList())

    private fun MangaReadChapter.toFirebaseChapter() = FirebaseChapter(url, name, mangaUrl)
    private fun FirebaseChapter.toMangaChapter() = MangaReadChapter(url!!, name!!, mangaUrl!!)

    fun getAllChapters() = chapterDoc
        ?.get(Source.DEFAULT)
        ?.await()
        ?.toObject(FirebaseAllChapter::class.java)
        ?.second
        ?.map { it.toMangaChapter() }

    private var allChapterFlowableListener: ListenerRegistration? = null

    fun detachChapterListener() {
        allChapterFlowableListener?.remove()
        allChapterFlowableListener = null
    }

    fun getAllChapterFlowable(): Flowable<List<MangaReadChapter>> = PublishSubject.create<List<MangaReadChapter>> { emitter ->
        allChapterFlowableListener?.remove()
        allChapterFlowableListener = chapterDoc?.addSnapshotListener { documentSnapshot, _ ->
            documentSnapshot?.toObject(FirebaseAllChapter::class.java)
                ?.second
                ?.map { it.toMangaChapter() }?.let { emitter(it) }
        }
        if (allChapterFlowableListener == null) emitter()
    }.toLatestFlowable().subscribeOn(Schedulers.io())

    fun addChapter(mangaModel: MangaReadChapter) = Completable.create { emitter ->
        chapterDoc
            ?.update("second", FieldValue.arrayUnion(mangaModel.toFirebaseChapter()))
            ?.addOnSuccessListener {
                emitter()
                Loged.d("Success!")
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

    fun removeChapter(mangaModel: MangaReadChapter) = Completable.create { emitter ->
        chapterDoc
            ?.update("second", FieldValue.arrayRemove(mangaModel.toFirebaseChapter()))
            ?.addOnSuccessListener {
                emitter()
                Loged.d("Success!")
            }?.addOnFailureListener {
                Loged.wtf("Failure!")
                emitter(it)
            }?.addOnCompleteListener {
                Loged.d("All done!")
            } ?: emitter()
    }

}

fun Context.dbAndFireManga(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = Flowables.combineLatest(
    dao.getAllManga(),
    FirebaseDb.getAllMangaFlowable()
) { db, fire -> (db + fire).distinctBy { it.mangaUrl }.map { it.toMangaModel() } }

fun Context.dbAndFireManga2(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = Flowables.combineLatest(
    dao.getAllManga(),
    FirebaseDb.getAllMangaFlowable()
) { db, fire -> (db + fire).groupBy(MangaDbModel::mangaUrl).map { it.value.maxBy(MangaDbModel::numChapters)!! }.map { it.toMangaModel() } }

fun Context.dbAndFireMangaSync(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = listOf(
    dao.getAllMangaSync(),
    FirebaseDb.getAllManga()?.requireNoNulls().orEmpty()
).flatten().distinctBy { it.mangaUrl }

fun Context.dbAndFireMangaSync2(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = listOf(
    dao.getAllMangaSync(),
    FirebaseDb.getAllManga()?.requireNoNulls().orEmpty()
).flatten().groupBy(MangaDbModel::mangaUrl).map { it.value.maxBy(MangaDbModel::numChapters)!! }

fun Context.dbAndFireMangaSync3(dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()) = listOf(
    dao.getAllMangaSync(),
    FirebaseDb.getAllManga2()?.requireNoNulls().orEmpty()
).flatten().groupBy(MangaDbModel::mangaUrl).map { it.value.maxBy(MangaDbModel::numChapters)!! }

fun Context.dbAndFireChapter(
    url: String,
    dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()
) = Flowables.combineLatest(
    dao.getReadChaptersById(url),
    FirebaseDb.getAllChapterFlowable()
) { db, fire -> (db + fire).distinctBy { it.url } }

fun Context.dbAndFireChapterNonFlow(
    url: String,
    dao: MangaDao = MangaDatabase.getInstance(this).mangaDao()
) = listOfNotNull(
    dao.getReadChaptersByIdNonFlow(url),
    FirebaseDb.getAllChapters()
).flatten().distinctBy { it.url }
