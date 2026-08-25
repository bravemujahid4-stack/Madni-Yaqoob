package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.*

/**
 * Material 3 Double-Confirmation Safety Dialog.
 *
 * Mandate: No record can be deleted on a single click. Every deletion requires
 * a 2-stage verification process. Soft-deleted items are safely archived in the
 * "Others Delete" (Recycle Bin) folder.
 */
@Composable
fun DoubleConfirmDeleteDialog(
    title: String = "Delete Confirmation",
    itemName: String,
    itemCode: String? = null,
    itemType: String? = null,
    additionalDetail: String? = null,
    isPermanent: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, if (isPermanent) MasRed.copy(alpha = 0.5f) else MasAmber.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("double_confirm_dialog")
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    } else {
                        slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                    }
                },
                label = "delete_step_transition"
            ) { step ->
                if (step == 1) {
                    // ========================================================
                    // STEP 1: Initial Intent Check
                    // ========================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(if (isPermanent) MasRedLight else MasAmberSoft, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPermanent) Icons.Default.DeleteForever else Icons.Default.DeleteOutline,
                                contentDescription = null,
                                tint = if (isPermanent) MasRed else MasAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PillBadge("Step 1 of 2", if (isPermanent) "red" else "amber")
                                if (itemType != null) {
                                    PillBadge(itemType, "blue")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (isPermanent) "Permanent Deletion" else "Move to Deleted Folder?",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }

                        // Target Item Info Box
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                if (itemCode != null) {
                                    Text(
                                        text = itemCode,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        color = MasRed
                                    )
                                }
                                Text(
                                    text = itemName,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                if (additionalDetail != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = additionalDetail,
                                        fontSize = 11.5.sp,
                                        color = MasMuted
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isPermanent) {
                                "This will permanently erase this record. It cannot be recovered once removed."
                            } else {
                                "This item will be safely moved to the Deleted Items (Recycle Bin) folder. You can restore it anytime or delete it permanently later."
                            },
                            fontSize = 12.sp,
                            color = MasMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("cancel_delete_step1"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", fontSize = 13.sp)
                            }
                            Button(
                                onClick = { currentStep = 2 },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(44.dp)
                                    .testTag("proceed_delete_step1"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPermanent) MasRed else MasAmber
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Continue (1/2) →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isPermanent) Color.White else Color.Black
                                )
                            }
                        }
                    }
                } else {
                    // ========================================================
                    // STEP 2: Double Confirmation (Final Safety Check)
                    // ========================================================
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .background(MasRedLight, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MasRed,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            PillBadge("Final Verification (2/2)", "red")
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Double Confirm Deletion",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MasRed,
                                textAlign = TextAlign.Center
                            )
                        }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MasRedLight),
                            border = BorderStroke(1.dp, MasRed.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "⚠️ Please Confirm",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp,
                                    color = MasRed
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isPermanent) {
                                        "Are you absolutely certain you want to permanently destroy '$itemName'? This action is irreversible."
                                    } else {
                                        "Are you sure you want to remove '$itemName'? It will be archived in the Deleted Items folder and removed from active accounting views."
                                    },
                                    fontSize = 12.sp,
                                    color = MasInk,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { currentStep = 1 },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .testTag("back_delete_step2"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("← Back", fontSize = 13.sp)
                            }
                            Button(
                                onClick = {
                                    onConfirm()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1.4f)
                                    .height(44.dp)
                                    .testTag("confirm_delete_step2"),
                                colors = ButtonDefaults.buttonColors(containerColor = MasRed),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPermanent) Icons.Default.DeleteForever else Icons.Default.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPermanent) "Yes, Delete Forever" else "Yes, Move to Trash",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
