package jp.techacademy.shohei.qa_app

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ListView

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_question_detail.*

import java.util.HashMap

class QuestionDetailActivity : AppCompatActivity() {

    private lateinit var mQuestion: Question
    private lateinit var mAdapter: QuestionDetailListAdapter
    private lateinit var mAnswerRef: DatabaseReference

    private var favcheck:String?=null


    //menu読み込み
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val user = FirebaseAuth.getInstance().currentUser
        if(user!=null) {
            val inflater: MenuInflater = menuInflater
            inflater.inflate(R.menu.favorite, menu)

            //お気に入りに登録状況によりon,off切り替え
            val uid=user!!.uid
            val database = FirebaseDatabase.getInstance()
            val fav = database.getReference("favorite")
            val favListener = object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // favoriteが作成されているか？
                    favcheck=dataSnapshot.getValue().toString()

                    if(favcheck=="null"){
                        menu.findItem(R.id.star).setIcon(android.R.drawable.star_big_off)
                        Log.d("test1",favcheck)

                    }else{
                        menu.findItem(R.id.star).setIcon(android.R.drawable.star_big_on)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            }
            fav.child(uid).child(mQuestion.questionUid).addValueEventListener(favListener)


            Log.d("test",fav.child(uid).ref.toString())
            return true
        }
            return false
    }
    //☆押された時の処理
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val user = FirebaseAuth.getInstance().currentUser
        //お気に入りに追加
        val database = FirebaseDatabase.getInstance()
        val fav = database.getReference("favorite")
        val uid=user!!.uid
        if (id == R.id.star) {

            if(favcheck=="null") {
                item.setIcon(android.R.drawable.star_big_on)
                val data = HashMap<String, String>()
                val ans = HashMap<String, String>()
                data["title"]=mQuestion.title
                data["body"]=mQuestion.body
                data["image"]=Base64.encodeToString(mQuestion.imageBytes, Base64.DEFAULT)
                data["name"]=mQuestion.name
                data["uid"]=mQuestion.questionUid
                //answer以外お気に入りへ移動
                val key=mQuestion.questionUid
                fav.child(uid).child(key).setValue(data)
                for(a in mQuestion.answers) {
                    ans["body"] = a.body
                    ans["name"] = a.name
                    ans["uid"] = a.uid
                    fav.child(uid).child(key).child(AnswersPATH).push().setValue(ans)

                }
                favcheck=="true"
            }else{
                item.setIcon(android.R.drawable.star_big_off)
                fav.child(uid).child(mQuestion.questionUid).setValue(null)
            }

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private val mEventListener = object : ChildEventListener {
        override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
            val map = dataSnapshot.value as Map<String, String>

            val answerUid = dataSnapshot.key ?: ""

            for (answer in mQuestion.answers) {
                // 同じAnswerUidのものが存在しているときは何もしない
                if (answerUid == answer.answerUid) {
                    return
                }
            }

            val body = map["body"] ?: ""
            val name = map["name"] ?: ""
            val uid = map["uid"] ?: ""

            val answer = Answer(body, name, uid, answerUid)
            mQuestion.answers.add(answer)
            mAdapter.notifyDataSetChanged()
        }

        override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onChildRemoved(dataSnapshot: DataSnapshot) {

        }

        override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

        }

        override fun onCancelled(databaseError: DatabaseError) {

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_question_detail)

        // 渡ってきたQuestionのオブジェクトを保持する
        val extras = intent.extras
        mQuestion = extras.get("question") as Question

        title = mQuestion.title

        // ListViewの準備
        mAdapter = QuestionDetailListAdapter(this, mQuestion)
        listView.adapter = mAdapter
        mAdapter.notifyDataSetChanged()

        fab.setOnClickListener {
            // ログイン済みのユーザーを取得する
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                // ログインしていなければログイン画面に遷移させる
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Questionを渡して回答作成画面を起動する
                // --- ここから ---
                val intent = Intent(applicationContext, AnswerSendActivity::class.java)
                intent.putExtra("question", mQuestion)
                startActivity(intent)
                // --- ここまで ---
            }
        }

        val dataBaseReference = FirebaseDatabase.getInstance().reference
        mAnswerRef = dataBaseReference.child(ContentsPATH).child(mQuestion.genre.toString()).child(mQuestion.questionUid).child(AnswersPATH)
        mAnswerRef.addChildEventListener(mEventListener)
    }
}