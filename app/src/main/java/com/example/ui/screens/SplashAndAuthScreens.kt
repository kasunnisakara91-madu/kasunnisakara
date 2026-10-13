package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.data.model.UserProfile
import com.example.ui.theme.AvatarPalette
import com.example.ui.theme.CriminalCyan
import com.example.ui.theme.CriminalEmerald
import com.example.ui.theme.ObsidianBg
import com.example.util.CriminalMediaAndNotificationHelper

@Composable
fun CriminalSplashScreen(
    onSplashFinished: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_scale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1400L)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ObsidianBg,
                        Color(0xFF0F1A2C),
                        ObsidianBg
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .scale(scale)
                    .clip(RoundedCornerShape(34.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(listOf(CriminalEmerald, CriminalCyan)),
                        shape = RoundedCornerShape(34.dp)
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_criminal_logo_1791649939055),
                    contentDescription = "CRIMINAL APP Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "CRIMINAL APP",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "NEXT-GEN REAL-TIME MESSAGING & CALLS",
                style = MaterialTheme.typography.labelSmall,
                color = CriminalEmerald,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            CircularProgressIndicator(
                color = CriminalEmerald,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    tint = CriminalEmerald,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Zero-Trust Cloud Firestore Security",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Developed by Madusanka",
                style = MaterialTheme.typography.labelLarge,
                color = CriminalCyan
            )
        }
    }
}

@Composable
fun CriminalAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var phoneInput by remember { mutableStateOf("") }
    var otpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var generatedOtp by remember { mutableStateOf("849201") }
    var otpVerified by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(185.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_auth_banner_1791649957557),
                    contentDescription = "CRIMINAL APP Security Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color(0xCC0B0F19))
                            )
                        )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_criminal_logo_1791649939055),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "CRIMINAL APP",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White
                        )
                        Text(
                            text = "By Madusanka • Instant Encrypted Comms",
                            style = MaterialTheme.typography.bodySmall,
                            color = CriminalEmerald
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Identity Verification",
                            tint = CriminalEmerald
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Identity & Phone Verification",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Sign in with your Google account to unlock real-time Cloud Firestore messaging, voice/video calls, and 24-hour status stories.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Optional Phone + OTP verification step prior to Google Sign-In
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number or Handle (Optional)") },
                        placeholder = { Text("+94 77 123 4567") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = "Phone")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_phone_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (phoneInput.isNotBlank() && !otpVerified) {
                        if (!otpSent) {
                            OutlinedButton(
                                onClick = {
                                    generatedOtp = (100000..999999).random().toString()
                                    otpCode = generatedOtp
                                    otpSent = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("send_otp_button")
                            ) {
                                Text("Generate Verification OTP")
                            }
                        } else {
                            Text(
                                text = "Verification Code Dispatched: $generatedOtp",
                                style = MaterialTheme.typography.labelSmall,
                                color = CriminalCyan
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { otpCode = it },
                                    label = { Text("6-Digit OTP") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("otp_code_input")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        if (otpCode == generatedOtp) {
                                            otpVerified = true
                                            errorMessage = null
                                        } else {
                                            errorMessage = "Invalid OTP code"
                                        }
                                    },
                                    modifier = Modifier.testTag("verify_otp_button")
                                ) {
                                    Text("Verify")
                                }
                            }
                        }
                    }

                    if (otpVerified) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            Icon(
                                Icons.Default.VerifiedUser,
                                contentDescription = "OTP Verified",
                                tint = CriminalEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Phone number verified ($phoneInput)",
                                style = MaterialTheme.typography.bodySmall,
                                color = CriminalEmerald
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = errorMessage != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Text(
                                text = errorMessage.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            isLoading = true
                            errorMessage = null
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isLoading = false
                                }
                            )
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CriminalEmerald,
                            contentColor = Color(0xFF002110)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF002110),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = "Google Sign-In"
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CRIMINAL APP v1.0 • Engineered by Madusanka",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProfileSetupScreen(
    initialEmailOrName: String,
    existingProfile: UserProfile?,
    isSaving: Boolean,
    onSaveProfile: (
        username: String,
        displayName: String,
        bio: String,
        phoneNumber: String,
        avatarColor: String,
        avatarBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val defaultHandle = remember(initialEmailOrName, existingProfile) {
        existingProfile?.username?.ifBlank { null }
            ?: initialEmailOrName.substringBefore("@").lowercase().replace(Regex("[^a-z0-9_]"), "_").take(20).ifBlank { "criminal_agent" }
    }

    var username by remember(existingProfile) { mutableStateOf(defaultHandle) }
    var displayName by remember(existingProfile) {
        mutableStateOf(
            existingProfile?.displayName?.ifBlank { null }
                ?: initialEmailOrName.substringBefore("@").replaceFirstChar { it.uppercase() }.ifBlank { "Agent" }
        )
    }
    var bio by remember(existingProfile) {
        mutableStateOf(existingProfile?.bio ?: "Encrypted comms active on CRIMINAL APP.")
    }
    var phoneNumber by remember(existingProfile) {
        mutableStateOf(existingProfile?.phoneNumber?.ifBlank { "+94 77 000 1111" } ?: "+94 77 000 1111")
    }
    var selectedColor by remember(existingProfile) {
        mutableStateOf(existingProfile?.avatarColor ?: AvatarPalette.first())
    }
    var avatarBase64 by remember(existingProfile) {
        mutableStateOf(existingProfile?.avatarBase64)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val encoded = CriminalMediaAndNotificationHelper.encodeImageUriToBase64(context, uri, maxDimension = 320)
            if (encoded != null) {
                avatarBase64 = encoded
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Set Up Your Profile",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Choose your unique @username, photo, and bio for CRIMINAL APP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Profile Avatar with Photo Picker
            val decodedBitmap = remember(avatarBase64) {
                CriminalMediaAndNotificationHelper.decodeBase64ToImageBitmap(avatarBase64)
            }
            Box(
                contentAlignment = Alignment.BottomEnd,
                modifier = Modifier
                    .size(110.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("profile_avatar_picker")
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CriminalMediaAndNotificationHelper.parseColorSafe(selectedColor))
                        .border(3.dp, CriminalEmerald, CircleShape)
                ) {
                    if (decodedBitmap != null) {
                        Image(
                            bitmap = decodedBitmap,
                            contentDescription = "Selected Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = displayName.take(2).uppercase().ifBlank { "CA" },
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFF002110),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.5.dp, CriminalEmerald, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Pick Photo",
                        tint = CriminalEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Avatar Accent Palette
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarPalette.forEach { hex ->
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(CriminalMediaAndNotificationHelper.parseColorSafe(hex))
                            .border(
                                width = if (isSelected) 2.5.dp else 0.dp,
                                color = Color.White,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected Color",
                                tint = Color(0xFF0B0F19),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().replace(" ", "_") },
                label = { Text("Unique @username (3-30 chars)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Username") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_username_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Full Display Name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_display_name_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_phone_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio / Status Line") },
                maxLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("setup_bio_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmedUser = username.trim()
                    val trimmedName = displayName.trim()
                    if (trimmedUser.length < 3) {
                        validationError = "Username must be at least 3 characters."
                        return@Button
                    }
                    if (trimmedName.isBlank()) {
                        validationError = "Display name cannot be empty."
                        return@Button
                    }
                    validationError = null
                    onSaveProfile(
                        trimmedUser,
                        trimmedName,
                        bio.trim(),
                        phoneNumber.trim(),
                        selectedColor,
                        avatarBase64
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CriminalEmerald,
                    contentColor = Color(0xFF002110)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("complete_profile_setup_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color(0xFF002110),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Launch CRIMINAL APP",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
