package com.example.mymemory

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mymemory.models.BoardSize
import com.example.mymemory.utils.BitmapScaler
import com.example.mymemory.utils.EXTRA_BOARD_SIZE
import com.example.mymemory.utils.isPermissionGranted
import com.example.mymemory.utils.requestPermission
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    companion object {
        private const val PICK_PHOTO_CODE = 655
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val READ_EXTERNAL_PHOTOS_CODE = 248
        private const val TAG = "CreateActivity"
        private const val MAX_ALLOWABLE_LENGTH = 14
        private  const val MIN_ALLOWABLE_LENGTH = 3

    }
    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btnSave: Button
    private  lateinit var  adapter: ImagePickerAdapter
    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1


    //uploading image is an asynchronous process
    //we get a download url for each image
    //if download url is equal to the number of the uris, we are done
    private val chosenImageUris = mutableListOf<Uri>() //defines where the resource lives
    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        //show back button
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pictures(0 / $numImagesRequired)"

        //set on click listener
        btnSave.setOnClickListener{
            saveDataToFirebase()
        }

        //set a maximum allowable length
        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_ALLOWABLE_LENGTH))
        etGameName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = shouldEnableSaveButton()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

            }

        })
        //adapter and layout manager
        //context and list of images
        adapter = ImagePickerAdapter(this, chosenImageUris, boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceHolderClicked() {
                //we will launch intent here
                //there are two types of intents
                //Implicit and explicit
                //when we ask for permissin we need to show a dialog
                if(isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)){
                    //if granted permission
                    launchIntentForPhotos()
                }
                else{
                    //request permission
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)

                }

            }

        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    private fun saveDataToFirebase() {
        val customGameName: String = etGameName.text.toString()
        Log.d(TAG, "Added the image")
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for((index, photoUri) in chosenImageUris.withIndex()){
            val imageByteArray = getImageByteArray(photoUri)
            //filepath where image is in a directory with the game name

            val filePath = "images/$customGameName/${System.currentTimeMillis()} - ${index}.jpg"
            val photoReference: StorageReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                    .continueWithTask {
                        //asynchronous process
                        photoUploadTask ->
                        Log.i(TAG, "Uploaded Bytes: ${photoUploadTask.result?.bytesTransferred}")
                        photoReference.downloadUrl
                    }.addOnCompleteListener {
                        downloadUrlTask ->
                        if(!downloadUrlTask.isSuccessful){
                           Log.e(TAG, "Exception with firebase !", downloadUrlTask.exception)

                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_LONG).show()
                            didEncounterError = true
                            return@addOnCompleteListener
                        }
                        if(didEncounterError){
                            return@addOnCompleteListener
                        }
                        val downloadUrl: String = downloadUrlTask.toString()
                        //image is successfully uploaded
                        uploadedImageUrls.add(downloadUrl)
                        Log.i(TAG, "Finished upload $photoUri" )
                        if(uploadedImageUrls.size == chosenImageUris.size){
                           handleAllImagesUploaded(customGameName, uploadedImageUrls)
                        }
                    }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        //upload to firestore
    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        }else{
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original Width ${originalBitmap.width}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()


    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray) {
        if(requestCode == READ_EXTERNAL_PHOTOS_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                launchIntentForPhotos()
            }
            else{
                Toast.makeText(this, "Inorder to create your custom game you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    //pass photos if done permission
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode != PICK_PHOTO_CODE || resultCode != Activity.RESULT_OK || data == null){
            Log.w(TAG, "Did not get data back")
            return
        }
        //1 photo
        val selectedUri: Uri? = data.data
        val clipData: ClipData? = data.clipData
        if(clipData != null){
            Log.i(TAG, "Number of images in clipdata ${clipData.itemCount}: $clipData")
            for(i in 0 until clipData.itemCount){
                //add image uri in chosen image uri
                val clipItem = clipData.getItemAt(i)
                if(chosenImageUris.size < numImagesRequired){
                    //check size
                    chosenImageUris.add(clipItem.uri)
                }
            }
        }
        //else
        else if(selectedUri != null){
            Log.i(TAG, "$selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        //update title of action bar
        supportActionBar?.title = "Choose pictures(${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = shouldEnableSaveButton()
    }


    //enabling the save button
    private fun shouldEnableSaveButton(): Boolean {
        if(chosenImageUris.size != numImagesRequired){
            return false
        }
        if(etGameName.text.isBlank() || etGameName.text.length < MIN_ALLOWABLE_LENGTH){
            return  false
        }
         return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        //images only in intent
        intent.type = "image/*"
        //select multiple images
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        //accessing files
        startActivityForResult(Intent.createChooser(intent, "Choose Pictures"), PICK_PHOTO_CODE )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}