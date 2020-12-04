package com.stargaze_developers.instagram

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.android.synthetic.main.activity_add_post.*

class AddStoryActivity : AppCompatActivity()
{

    private var myUrl = ""
    private var myPostUrl = ""

    private var imageUri: Uri? = null
    private var imagePostUri: Uri? = null

    private var storageStoryPicRef: StorageReference? = null
    private var storagePostPicRef: StorageReference? = null



    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_post)

        storageStoryPicRef = FirebaseStorage.getInstance().reference.child("Story Pictures")

//        -----------------------------for getting the story picture to POST--------------------
        storagePostPicRef = FirebaseStorage.getInstance().reference.child("Posts Pictures")
        //        -----------------------------for getting the story picture to POST--------------------


        save_new_post_btn.setOnClickListener{ uploadImage() }


        close_add_post_btn.setOnClickListener {
            val intent = Intent(this@AddStoryActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        CropImage.activity()
            .setAspectRatio(2,1)
            .start(this@AddStoryActivity)


    }




    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null)
        {
            val result = CropImage.getActivityResult(data)
            imageUri = result.uri
            imagePostUri = result.uri

            image_post.setImageURI(imagePostUri)

            uploadStory()




        }
    }

    private fun uploadStory()
    {



                val fileRef = storageStoryPicRef!!.child(System.currentTimeMillis().toString() + ".jpg")


                var uploadTask: StorageTask<*>
                uploadTask = fileRef.putFile(imageUri!!)


                uploadTask.continueWithTask(Continuation <UploadTask.TaskSnapshot, Task<Uri>>{ task ->
                    if(!task.isSuccessful)
                    {
                        task.exception?.let {
                            throw it
//                            progressDialog.dismiss()

                        }
                    }
                    return@Continuation fileRef.downloadUrl
                }).addOnCompleteListener (OnCompleteListener<Uri> { task ->
                    if (task.isSuccessful)
                    {
                        val downloadUrl = task.result
                        myUrl = downloadUrl.toString()


                        val ref = FirebaseDatabase.getInstance().reference
                            .child("Story")
                            .child(FirebaseAuth.getInstance().currentUser!!.uid)
                        val storyId = (ref.push().key).toString()

                        val timeEnd = System.currentTimeMillis() + 86400000  //------------story for only one day-------------

                        val  storyMap = HashMap<String, Any>()
                        storyMap["userid"] = FirebaseAuth.getInstance().currentUser!!.uid
                        storyMap["timestart"] = ServerValue.TIMESTAMP
                        storyMap["timeend"] = timeEnd
                        storyMap["imageurl"] = myUrl
                        storyMap["storyid"] = storyId

                        ref.child(storyId).updateChildren(storyMap)


                    }

                } )

    }





    //-----------------------------------POST=---------------------------
    private fun uploadImage()
    {
        when {
            imagePostUri == null -> Toast.makeText(this, "Go back and select an image", Toast.LENGTH_LONG)
                .show()


            else -> {

                val progressDialog = ProgressDialog(this)
                progressDialog.setTitle("Adding Story")
                progressDialog.setMessage("Please wait...")
                progressDialog.show()

                val filePostRef =
                    storagePostPicRef!!.child(System.currentTimeMillis().toString() + ".jpg")

                var uploadPostTask: StorageTask<*>
                uploadPostTask = filePostRef.putFile(imagePostUri!!)

                uploadPostTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let {
                            throw it
                            progressDialog.dismiss()
                        }
                    }
                    return@Continuation filePostRef.downloadUrl
                }).addOnCompleteListener(OnCompleteListener<Uri> { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result
                        myPostUrl = downloadUrl.toString()
                        val postRef = FirebaseDatabase.getInstance().reference.child("Posts")
                        val postId = postRef.push().key

                        val postMap = HashMap<String, Any>()
                        postMap["postid"] = postId!!
                        postMap["description"] = description_post.text.toString().toLowerCase()
                        postMap["publisher"] = FirebaseAuth.getInstance().currentUser!!.uid
                        postMap["postimage"] = myPostUrl

                        postRef.child(postId).updateChildren(postMap)

                        Toast.makeText(this, "Story added successfully", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@AddStoryActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()

                        progressDialog.dismiss()
                    } else {
                        Toast.makeText(this, "Story failed to be uploaded", Toast.LENGTH_LONG)
                            .show()

                        val intent = Intent(this@AddStoryActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()


                        progressDialog.dismiss()
                    }

                })
            }


        }

    }

}
