package com.example.hms.auth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;


public class authManager {
    private FirebaseAuth auth;

    public authManager(){
        auth=FirebaseAuth.getInstance();
    }
    // getter
    public FirebaseAuth getAuth(){
        return auth;
    }
     public  void register(String email ,String password ,Authcallback callback){
        auth.createUserWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        callback.onSuccess();
                    }else{
                        callback.onFailure(task.getException().getMessage());
                    }
                });

     }
     public  void login(String email ,String password ,Authcallback callback){
        auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if(task.isSuccessful()){
                        callback.onSuccess();
                    }else{
                        callback.onFailure(task.getException().getMessage());
                    }
                });


     }
     public void loginwithgoogle(String idToken,Authcallback callback) {
         AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
         auth.signInWithCredential(credential)
                 .addOnCompleteListener(task -> {
                     if (task.isSuccessful()) {
                         callback.onSuccess();
                     } else {
                         callback.onFailure(task.getException().getMessage());
                     }
                 });

     }
     public interface Authcallback{
         void onSuccess();
         void onFailure(String message);
     }
}
