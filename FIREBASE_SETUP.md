# Keepsy Firebase Production Setup Guide

To enable production-grade authentication features (Automatic Welcome Emails), follow these steps in your Firebase Console.

## 1. Automatic Welcome Email (Cloud Functions)

Since Firebase Authentication doesn't send custom HTML emails natively, we use a Cloud Function.

### Step-by-Step Configuration:

1.  **Enable Blaze Plan**: Cloud Functions require the "Pay-as-you-go" plan (Blaze). There is a generous free tier.
2.  **Initialize Functions**: Run `firebase init functions` in your terminal if you haven't.
3.  **Use the following `index.js` code**:

```javascript
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const nodemailer = require("nodemailer");

admin.initializeApp();

// CONFIGURE YOUR SMTP PROVIDER HERE (e.g., Gmail, SendGrid, Mailgun)
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: 'YOUR_EMAIL@gmail.com',
        pass: 'YOUR_APP_PASSWORD'
    }
});

exports.sendWelcomeEmail = functions.auth.user().onCreate((user) => {
    const email = user.email;
    const displayName = user.displayName || "Friend";

    const mailOptions = {
        from: '"Team Keepsy" <no-reply@keepsy-app.firebaseapp.com>',
        to: email,
        subject: 'Welcome to Keepsy 🎉',
        html: `
            <div style="background-color: #0B1220; color: #FFFFFF; padding: 40px; font-family: sans-serif; border-radius: 16px;">
                <h1 style="color: #6366F1;">Hello ${displayName},</h1>
                <p style="font-size: 18px; line-height: 1.6;">Welcome to Keepsy!</p>
                <p style="color: #94A3B8;">Thank you for joining our community. Keepsy helps you remember where you've kept your important belongings, organize your spaces, and never waste time searching again.</p>
                
                <div style="margin: 30px 0; border-left: 4px solid #00E0D1; padding-left: 20px;">
                    <p><strong>You can now:</strong></p>
                    <ul style="color: #94A3B8;">
                        <li>Organize your Spaces</li>
                        <li>Create Subspaces</li>
                        <li>Save important Items</li>
                        <li>Search instantly</li>
                        <li>Track movement history</li>
                    </ul>
                </div>

                <p style="font-size: 16px;">We're excited to have you with us. <br>Happy organizing!</p>
                
                <p style="color: #6366F1; font-weight: bold;">— Team Keepsy</p>
                
                <hr style="border-top: 1px solid #1E293B; margin-top: 40px;">
                <p style="font-size: 12px; color: #64748B; text-align: center;">
                    You are receiving this email because you created a Keepsy account.
                </p>
            </div>
        `
    };

    return transporter.sendMail(mailOptions)
        .then(() => console.log('Welcome email sent to:', email))
        .catch((error) => console.error('Error sending welcome email:', error));
});
```

4.  **Deploy**: Run `firebase deploy --only functions`.

---

## 2. Alternative: "Trigger Email" Extension (No Coding)

If you prefer not to write code, you can use the official **"Trigger Email from Firestore"** extension.

1.  Go to **Firebase Extensions** in the console.
2.  Install **"Trigger Email"**.
3.  Set the `Users` collection in the app to sync with the extension.
4.  In the `FirebaseService.kt` `updateUserProfile` method, we already update the `users` collection.
5.  Configure the extension to send the email when a new document is added to `users`.

---

## 3. Email Verification Template

1.  Go to **Firebase Console** -> **Authentication** -> **Templates**.
2.  Select **Email address verification**.
3.  Customize the template to match the Keepsy tone.
4.  Ensure the "Sender name" is set to "Team Keepsy".

## 4. Password Reset Template

1.  Go to **Firebase Console** -> **Authentication** -> **Templates**.
2.  Select **Password reset**.
3.  Ensure the redirect URL points correctly to your app or a verified domain.
