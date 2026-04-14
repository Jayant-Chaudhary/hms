package com.example.hms.auth;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;


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

     public void sendEmailVerification(Authcallback callback) {
         FirebaseUser user = auth.getCurrentUser();
         if (user == null) {
             callback.onFailure("No signed-in user found");
             return;
         }
         user.sendEmailVerification()
                 .addOnCompleteListener(task -> {
                     if (task.isSuccessful()) {
                         callback.onSuccess();
                     } else {
                         callback.onFailure(task.getException() != null
                                 ? task.getException().getMessage()
                                 : "Could not send verification email");
                     }
                 });
     }

     public void sendPasswordReset(String email, Authcallback callback) {
         auth.sendPasswordResetEmail(email)
                 .addOnCompleteListener(task -> {
                     if (task.isSuccessful()) {
                         callback.onSuccess();
                     } else {
                         callback.onFailure(task.getException() != null
                                 ? task.getException().getMessage()
                                 : "Could not send reset email");
                     }
                 });
     }

     public interface Authcallback{
         void onSuccess();
         void onFailure(String message);
     }
}
