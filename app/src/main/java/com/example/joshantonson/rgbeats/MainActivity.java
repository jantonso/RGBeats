package com.example.joshantonson.rgbeats;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.MediaStore.MediaColumns;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends Activity {

    final static private int CAMERA_PIC_REQUEST = 1337;
    final static private int GALLERY_REQ = 1338;

    private char[] colors=new char[3];
    private boolean[] picture=new boolean[3];
    private int currp=0;

    private int filenamecounter = 1;
    private String newsongname;
    private boolean newsong=false;
    private AssetFileDescriptor afd;
    private MediaPlayer mp;
    private SoundPool sp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        mp=new MediaPlayer();
        sp=new SoundPool(5,AudioManager.STREAM_MUSIC,0);
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
            @Override
            public void onCompletion(MediaPlayer mp){
                if(newsong){
                    mp.stop();
                    mp.reset();
                    //mp.release();
                    try {
                        afd=getAssets().openFd("Sounds/"+newsongname+".mp3");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        mp.prepare();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    newsong=false;
                    mp.start();
                }
                else{
                    mp.start();
                }
            }

        });

        sp.setOnLoadCompleteListener(new OnLoadCompleteListener(){
            @Override
            public void onLoadComplete(SoundPool sp, int sampleid, int status){
                if(status==0){
                    sp.play(sampleid, 1, 1, currp-1, -1, 1);
                }
            }
        });

        ImageView pictureView1 = (ImageView) findViewById(R.id.imageView1);
        pictureView1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!picture[0]){
                    takePicture();
                }
            }
        });

        ImageView pictureView2 = (ImageView) findViewById(R.id.imageView2);
        pictureView2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[0] && !picture[1]){
                    takePicture();
                }
            }
        });

        ImageView pictureView3 = (ImageView) findViewById(R.id.imageView3);
        pictureView3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[0] && picture[1] && !picture[2]){
                    takePicture();
                }
            }
        });

        final ImageView nav_buttons_cameraroll = (ImageView) findViewById(R.id.nav_buttons_cameraroll);
        nav_buttons_cameraroll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getPicture();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){

        super.onActivityResult(requestCode, resultCode, data);
        if (currp>2) currp=2; // when there are already 3 pictures, replace the third one
        if (requestCode == CAMERA_PIC_REQUEST) {
            // Get the picture that was taken by the camera
            Bitmap newPicture = (Bitmap) data.getExtras().get("data");

            Bitmap resizedBitmap = analyzeRGBValues(newPicture);
            if (!picture[0]){ // first pic taken
                createFirstContents(colors[0], resizedBitmap);
            } else if (!picture[1]){ // 2nd pic taken
                createSecondContents(colors[1], resizedBitmap);
            } else if (!picture[2]){ // third pic taken
                createThirdContents(colors[2], resizedBitmap);
            }
            currp++;filenamecounter++;
            savePicture(newPicture);
        } else if(requestCode == GALLERY_REQ){
            if(resultCode == RESULT_OK){
                // Get the picture from the user's gallery
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaColumns.DATA};

                Cursor cursor = getContentResolver().query(selectedImage,
                        filePathColumn, null, null, null);
                cursor.moveToFirst();

                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                String filePath = cursor.getString(columnIndex);
                cursor.close();

                Bitmap newPicture = BitmapFactory.decodeFile(filePath);

                Bitmap resizedBitmap = analyzeRGBValues(newPicture);
                if (!picture[0]){ // first pic
                    createFirstContents(colors[0], resizedBitmap);
                } else if (!picture[1]){ // 2nd pic
                    createSecondContents(colors[1], resizedBitmap);
                } else if (!picture[2]) { // 3rd pic
                    createSecondContents(colors[2], resizedBitmap);
                }
                currp++;filenamecounter++;
            }
        }
        playSound();
    }

    private void playSound() {
        String filename;
        if (currp<3 && colors[2]>0) filename=new String(colors);
        else if (currp==2 && colors[1]=='1') filename=new String(colors,0,1);
        else filename=new String(colors,0,currp);

        Log.d("FILENAME", filename);

        if (mp.isPlaying())
        {
            newsongname=filename;
            newsong=true;
        }
        else
        {
            newsongname=filename;
            try {
                afd=getAssets().openFd("Sounds/"+filename+".mp3");
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                try {
                    mp.prepare();
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            newsong=false;
            mp.start();
        }
    }

    private void pauseplay(){
        if(mp.isPlaying()){
            mp.pause();
        }
        else{
            mp.start();
        }
    }

    private void killtrack(int i, View v){
        if(i==0){
            if (mp.isPlaying()){
                try {
                    mp.stop();
                    mp.reset();
                    afd=getAssets().openFd("Sounds/"+newsongname+".mp3");
                    mp.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                    mp.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            colors=new char[3];
            currp=0;
            picture = new boolean[3];

            deleteFirstContents();
            deleteSecondContents();
            deleteThirdContents();
        }else if(i==1){ // 2nd picture
            colors[i]='1';
            currp=1;
            picture[1]=false;
            playSound();

            deleteSecondContents();
        }else{ // 3rd picture
            colors[i]=0;
            currp=2;
            picture[2]=false;
            playSound();

            deleteThirdContents();
        }
    }

    private Bitmap analyzeRGBValues(Bitmap newPicture) {
        int[] pixels = new int[newPicture.getHeight()*newPicture.getWidth()];
        newPicture.getPixels(pixels, 0, newPicture.getWidth(), 0, 0,
                newPicture.getWidth(), newPicture.getHeight());
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(newPicture, 140, 140, false);
        int red; int blue; int green; int totalRed = 0; int totalGreen = 0; int totalBlue = 0;
        int height = newPicture.getHeight();
        int width = newPicture.getWidth();
        for (int i = 0; i < height*width; i++){
            red = (pixels[i] & 0xFF0000) >> 16;
            green = (pixels[i] & 0xFF00) >> 8;
            blue = (pixels[i] & 0xFF);
            totalRed += red;
            totalBlue += blue;
            totalGreen += green;
        }
        totalRed = totalRed / (height*width);
        totalBlue = totalBlue / (height*width);
        totalGreen = totalGreen / (height*width);
        int difference1 = totalRed - totalGreen;
        int difference2 = totalRed - totalBlue;
        if ((-20 < difference1 && difference1 < 20) && (-20 < difference2 && difference2 < 20)){
            Random rand = new Random();
            int randomNumber = rand.nextInt(3-0);
            if (randomNumber == 0) {
                colors[currp] = 'R';
            } else if (randomNumber == 1){
                colors[currp] = 'G';
            } else {colors[currp] = 'B';}
        } else if (totalRed >= totalBlue && totalRed >= totalGreen){
            colors[currp] = 'R';
        } else if (totalGreen >= totalBlue && totalGreen >= totalRed){
            colors[currp] = 'G';
        } else {
            colors[currp] = 'B';
        }
        return resizedBitmap;
    }

    private void createFirstContents(char image_color, Bitmap resizedBitmap) {
        ImageView pictureView1 = (ImageView) findViewById(R.id.imageView1);
        if (image_color == 'R'){pictureView1.setImageResource(R.drawable.rock_rhythm_and_bass);}
        else if (image_color == 'G'){pictureView1.setImageResource(R.drawable.electro_rhythm_and_bass);}
        else {pictureView1.setImageResource(R.drawable.jazz_rhythm_and_bass);}

        // Create Picture 1
        ImageView newPicture1 = new ImageView(this);
        newPicture1.setImageBitmap(resizedBitmap);
        newPicture1.setId(R.id.newPicture1);

        RelativeLayout.LayoutParams paramsPicture1 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPicture1.addRule(RelativeLayout.ALIGN_LEFT, pictureView1.getId());
        paramsPicture1.addRule(RelativeLayout.ALIGN_TOP, pictureView1.getId());
        paramsPicture1.addRule(RelativeLayout.ALIGN_BOTTOM, pictureView1.getId());

        // Create Play Button 1
        ImageView newPlay1 = new ImageView(this);
        newPlay1.setImageResource(R.drawable.button__play);
        newPlay1.setId(R.id.play1);
        newPlay1.setClickable(true);
        newPlay1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[0] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPlay1 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPlay1.addRule(RelativeLayout.RIGHT_OF, newPicture1.getId());
        paramsPlay1.addRule(RelativeLayout.ALIGN_TOP, newPicture1.getId());
        paramsPlay1.leftMargin = 190;
        paramsPlay1.topMargin = 15;

        // Create Pause Button 1
        ImageView newPause1 = new ImageView(this);
        newPause1.setImageResource(R.drawable.button__pause);
        newPause1.setId(R.id.pause1);
        newPause1.setClickable(true);
        newPause1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[0] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPause1 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPause1.addRule(RelativeLayout.RIGHT_OF, newPlay1.getId());
        paramsPause1.addRule(RelativeLayout.ALIGN_TOP, newPlay1.getId());
        paramsPause1.leftMargin = 7;

        // Create Stop Button 1
        ImageView newStop1 = new ImageView(this);
        newStop1.setImageResource(R.drawable.button__stop);
        newStop1.setId(R.id.stop1);
        newStop1.setClickable(true);
        newStop1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[0] == true){
                    killtrack(0,view);
                }
            }
        });

        RelativeLayout.LayoutParams paramsStop1 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsStop1.addRule(RelativeLayout.RIGHT_OF, newPause1.getId());
        paramsStop1.addRule(RelativeLayout.ALIGN_TOP, newPause1.getId());
        paramsStop1.leftMargin = 7;

        // Add Picture1, Play Button1, Pause Button1, Stop Button1 to relative layout
        RelativeLayout rl1 = (RelativeLayout) findViewById(R.id.rl1);
        rl1.addView(newPicture1, paramsPicture1);
        rl1.addView(newPlay1, paramsPlay1);
        rl1.addView(newPause1, paramsPause1);
        rl1.addView(newStop1, paramsStop1);
        sendViewToBack(newPicture1);

        picture[0] = true;
    }

    private void createSecondContents(char image_color, Bitmap resizedBitmap) {
        ImageView pictureView2 = (ImageView) findViewById(R.id.imageView2);
        if (image_color == 'R'){pictureView2.setImageResource(R.drawable.rock_accompaniment);}
        else if (image_color == 'G'){pictureView2.setImageResource(R.drawable.electro_accompaniment);}
        else {pictureView2.setImageResource(R.drawable.jazz_accompaniment);}

        // Create Picture 2
        ImageView newPicture2 = new ImageView(this);
        newPicture2.setImageBitmap(resizedBitmap);
        newPicture2.setId(R.id.newPicture2);

        RelativeLayout.LayoutParams paramsPicture2 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPicture2.addRule(RelativeLayout.ALIGN_LEFT, pictureView2.getId());
        paramsPicture2.addRule(RelativeLayout.ALIGN_TOP, pictureView2.getId());
        paramsPicture2.addRule(RelativeLayout.ALIGN_BOTTOM, pictureView2.getId());

        // Create Play Button 2
        ImageView newPlay2 = new ImageView(this);
        newPlay2.setImageResource(R.drawable.button__play);
        newPlay2.setId(R.id.play2);
        newPlay2.setClickable(true);
        newPlay2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[1] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPlay2 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPlay2.addRule(RelativeLayout.RIGHT_OF, newPicture2.getId());
        paramsPlay2.addRule(RelativeLayout.ALIGN_TOP, newPicture2.getId());
        paramsPlay2.leftMargin = 190;
        paramsPlay2.topMargin = 15;

        // Create Pause Button 2
        ImageView newPause2 = new ImageView(this);
        newPause2.setImageResource(R.drawable.button__pause);
        newPause2.setId(R.id.pause2);
        newPause2.setClickable(true);
        newPause2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[1] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPause2 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPause2.addRule(RelativeLayout.RIGHT_OF, newPlay2.getId());
        paramsPause2.addRule(RelativeLayout.ALIGN_TOP, newPlay2.getId());
        paramsPause2.leftMargin = 7;

        // Create Stop Button 2
        ImageView newStop2 = new ImageView(this);
        newStop2.setImageResource(R.drawable.button__stop);
        newStop2.setId(R.id.stop2);
        newStop2.setClickable(true);
        newStop2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[1] == true){
                    killtrack(1,view);
                }
            }
        });

        RelativeLayout.LayoutParams paramsStop2 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsStop2.addRule(RelativeLayout.RIGHT_OF, newPause2.getId());
        paramsStop2.addRule(RelativeLayout.ALIGN_TOP, newPause2.getId());
        paramsStop2.leftMargin = 7;

        // Add Picture1, Play Button2, Pause Button2, Stop Button2 to relative layout
        RelativeLayout rl2 = (RelativeLayout) findViewById(R.id.rl2);
        rl2.addView(newPicture2, paramsPicture2);
        rl2.addView(newPlay2, paramsPlay2);
        rl2.addView(newPause2, paramsPause2);
        rl2.addView(newStop2, paramsStop2);
        sendViewToBack(newPicture2);

        picture[1] = true;
    }

    private void createThirdContents(char image_color, Bitmap resizedBitmap) {
        ImageView pictureView3 = (ImageView) findViewById(R.id.imageView3);
        if (image_color == 'R'){pictureView3.setImageResource(R.drawable.rock_lead);}
        else if (image_color == 'G'){pictureView3.setImageResource(R.drawable.electro_lead);}
        else {pictureView3.setImageResource(R.drawable.jazz_lead);}

        // Create Picture 3
        ImageView newPicture3 = new ImageView(this);
        newPicture3.setImageBitmap(resizedBitmap);
        newPicture3.setId(R.id.newPicture3);

        RelativeLayout.LayoutParams paramsPicture3 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPicture3.addRule(RelativeLayout.ALIGN_LEFT, pictureView3.getId());
        paramsPicture3.addRule(RelativeLayout.ALIGN_TOP, pictureView3.getId());
        paramsPicture3.addRule(RelativeLayout.ALIGN_BOTTOM, pictureView3.getId());

        // Create Play Button 3
        ImageView newPlay3 = new ImageView(this);
        newPlay3.setImageResource(R.drawable.button__play);
        newPlay3.setId(R.id.play3);
        newPlay3.setClickable(true);
        newPlay3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[2] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPlay3 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPlay3.addRule(RelativeLayout.RIGHT_OF, newPicture3.getId());
        paramsPlay3.addRule(RelativeLayout.ALIGN_TOP, newPicture3.getId());
        paramsPlay3.leftMargin = 190;
        paramsPlay3.topMargin = 15;

        // Create Pause Button 3
        ImageView newPause3 = new ImageView(this);
        newPause3.setImageResource(R.drawable.button__pause);
        newPause3.setId(R.id.pause3);
        newPause3.setClickable(true);
        newPause3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[2] == true){
                    pauseplay();
                }
            }
        });

        RelativeLayout.LayoutParams paramsPause3 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsPause3.addRule(RelativeLayout.RIGHT_OF, newPlay3.getId());
        paramsPause3.addRule(RelativeLayout.ALIGN_TOP, newPlay3.getId());
        paramsPause3.leftMargin = 7;

        // Create Stop Button 3
        ImageView newStop3 = new ImageView(this);
        newStop3.setImageResource(R.drawable.button__stop);
        newStop3.setId(R.id.stop3);
        newStop3.setClickable(true);
        newStop3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (picture[2] == true){
                    killtrack(2,view);
                }
            }
        });

        RelativeLayout.LayoutParams paramsStop3 =
                new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        paramsStop3.addRule(RelativeLayout.RIGHT_OF, newPause3.getId());
        paramsStop3.addRule(RelativeLayout.ALIGN_TOP, newPause3.getId());
        paramsStop3.leftMargin = 7;

        // Add Picture1, Play Button3, Pause Button3, Stop Button3 to relative layout
        RelativeLayout rl3 = (RelativeLayout) findViewById(R.id.rl3);
        rl3.addView(newPicture3, paramsPicture3);
        rl3.addView(newPlay3, paramsPlay3);
        rl3.addView(newPause3, paramsPause3);
        rl3.addView(newStop3, paramsStop3);
        sendViewToBack(newPicture3);

        picture[2] = true;
    }

    private void deleteFirstContents() {
        // delete the 1st picture
        RelativeLayout rl1 = (RelativeLayout) findViewById(R.id.rl1);
        ImageView pictureView1 = (ImageView) findViewById(R.id.imageView1);
        pictureView1.setImageResource(R.drawable.camera_button);
        ImageView picture1 = (ImageView) findViewById(R.id.newPicture1);
        rl1.removeView(picture1);

        // delete 1st play/pause/stop buttons
        ImageView playView1 = (ImageView) findViewById(R.id.play1);
        ImageView pauseView1 = (ImageView) findViewById(R.id.pause1);
        ImageView stopView1 = (ImageView) findViewById(R.id.stop1);
        rl1.removeView(playView1);
        rl1.removeView(pauseView1);
        rl1.removeView(stopView1);
    }

    private void deleteSecondContents() {
        // delete the 2nd picture
        RelativeLayout rl2 = (RelativeLayout) findViewById(R.id.rl2);
        ImageView pictureView2 = (ImageView) findViewById(R.id.imageView2);
        pictureView2.setImageResource(R.drawable.camera_button);
        ImageView picture2 = (ImageView) findViewById(R.id.newPicture2);
        rl2.removeView(picture2);

        // delete 2nd play/pause/stop buttons
        ImageView playView2 = (ImageView) findViewById(R.id.play2);
        ImageView pauseView2 = (ImageView) findViewById(R.id.pause2);
        ImageView stopView2 = (ImageView) findViewById(R.id.stop2);
        rl2.removeView(playView2);
        rl2.removeView(pauseView2);
        rl2.removeView(stopView2);
    }

    private void deleteThirdContents() {
        // delete the 3rd picture
        RelativeLayout rl3 = (RelativeLayout) findViewById(R.id.rl3);
        ImageView pictureView3 = (ImageView) findViewById(R.id.imageView3);
        pictureView3.setImageResource(R.drawable.camera_button);
        ImageView picture3 = (ImageView) findViewById(R.id.newPicture3);
        rl3.removeView(picture3);

        // delete 3rd play/pause/stop buttons
        ImageView playView3 = (ImageView) findViewById(R.id.play3);
        ImageView pauseView3 = (ImageView) findViewById(R.id.pause3);
        ImageView stopView3 = (ImageView) findViewById(R.id.stop3);
        rl3.removeView(playView3);
        rl3.removeView(pauseView3);
        rl3.removeView(stopView3);
    }

    private void takePicture() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST);
    }

    private void getPicture() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQ);
    }

    private void savePicture(Bitmap bmp){
        MediaStore.Images.Media.insertImage(getContentResolver(), bmp,
                "rgbimg"+((Integer)(filenamecounter)).toString()+".jpg", "Taken by RGBeats");
    }

    private static void sendViewToBack(final View child) {
        final ViewGroup parent = (ViewGroup)child.getParent();
        if (null != parent) {
            parent.removeView(child);
            parent.addView(child, 0);
        }
    }
}
