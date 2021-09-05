package io.capstone.ludendorff.features.assignment

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import io.capstone.ludendorff.api.Backend
import io.capstone.ludendorff.api.request.NotificationRequest
import io.capstone.ludendorff.api.exception.PreconditionFailedException
import io.capstone.ludendorff.api.exception.UnauthorizedException
import io.capstone.ludendorff.features.asset.Asset
import io.capstone.ludendorff.features.core.backend.Response
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.tasks.await
import okhttp3.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssignmentRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val backend: Backend
) {

    suspend fun create(assignment: Assignment, targetDeviceToken: String?): Response<Response.Action> {
        return try {
            firestore.runBatch {
                it.set(firestore.collection(Assignment.COLLECTION)
                    .document(assignment.assignmentId), assignment)

                assignment.asset?.assetId?.let { id ->
                    it.update(firestore.collection(Asset.COLLECTION)
                        .document(id), Asset.FIELD_STATUS, Asset.Status.OPERATIONAL)
                }
            }.await()

            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            if (token == null || targetDeviceToken == null)
                Response.Error(NullPointerException(), Response.Action.CREATE)

            val notificationRequest = NotificationRequest(
                token = token!!,
                deviceToken = targetDeviceToken!!,
                notificationTitle = NotificationRequest.NOTIFICATION_ASSIGNED_ASSET_TITLE,
                notificationBody = NotificationRequest.NOTIFICATION_ASSIGNED_ASSET_BODY,
                data = mapOf(NotificationRequest.FIELD_DATA_PAYLOAD to assignment.assignmentId)
            )

            val response = backend.newNotificationPost(notificationRequest)
            when(response.code()) {
                200 -> Response.Success(Response.Action.CREATE)
                401 -> Response.Error(UnauthorizedException(), Response.Action.CREATE)
                412 -> Response.Error(PreconditionFailedException(), Response.Action.CREATE)
                else -> Response.Error(Exception(), Response.Action.CREATE)
            }

        } catch (exception: FirebaseFirestoreException) {
            Response.Error(exception)
        } catch (exception: Exception) {
            Response.Error(exception)
        }
    }

    suspend fun update(assignment: Assignment,
                       targetDeviceToken: String?,
                       previousUserId: String?,
                       previousAssetId: String?): Response<Response.Action> {
        return try {
            firestore.runBatch {
                it.set(firestore.collection(Assignment.COLLECTION)
                    .document(assignment.assignmentId), assignment)

                /**
                 * The assets specified have been changed, we need to change
                 * the status of the previous and the new one accordingly.
                 */
                if (previousAssetId != null &&
                        previousAssetId != assignment.asset?.assetId) {
                    it.update(firestore.collection(Asset.COLLECTION)
                        .document(previousAssetId), Asset.FIELD_STATUS, Asset.Status.IDLE)

                    assignment.asset?.assetId?.let { newAssetId ->
                        it.update(firestore.collection(Asset.COLLECTION)
                            .document(newAssetId), Asset.FIELD_STATUS,
                            Asset.Status.OPERATIONAL)
                    }
                }
            }.await()

            /**
             * The user hasn't been requested to be changed
             * so we'll finish the operation now.
             */
            if (previousUserId == null ||
                    previousUserId == assignment.user?.userId)
                        Response.Success(Response.Action.UPDATE)

            /**
             * The user has been changed, we'll request
             * the Deshi to send a notification to the new user.
             * We'll need the id token of the current user so that
             * he's/she's identity we'll be verified by Deshi
             */
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            if (token == null || targetDeviceToken == null)
                Response.Error(UnauthorizedException(), Response.Action.UPDATE)

            val notificationRequest = NotificationRequest(
                token = token!!,
                deviceToken = targetDeviceToken!!,
                notificationTitle = NotificationRequest.NOTIFICATION_ASSIGNED_ASSET_TITLE,
                notificationBody = NotificationRequest.NOTIFICATION_ASSIGNED_ASSET_BODY,
                data = mapOf(NotificationRequest.FIELD_DATA_PAYLOAD to assignment.assignmentId)
            )

            val response = backend.newNotificationPost(notificationRequest)
            when(response.code()) {
                // HTTP Status: OK
                200 -> Response.Success(Response.Action.UPDATE)
                // HTTP Status: Unauthorized
                401 -> Response.Error(UnauthorizedException(), Response.Action.UPDATE)
                // HTTP Status: Precondition Failed
                412 -> Response.Error(PreconditionFailedException(), Response.Action.UPDATE)
                // HTTP Status: General Error
                else -> Response.Error(Exception(), Response.Action.UPDATE)
            }

        } catch (exception: FirebaseFirestoreException) {
            Response.Error(exception)
        } catch (exception: Exception) {
            Response.Error(exception)
        }
    }

    suspend fun remove(assignment: Assignment): Response<Response.Action> {
        return try {
            firestore.runBatch {
                it.delete(firestore.collection(Assignment.COLLECTION)
                    .document(assignment.assignmentId))

                assignment.asset?.assetId?.let { id ->
                    it.update(firestore.collection(Asset.COLLECTION)
                        .document(id), Asset.FIELD_STATUS, Asset.Status.IDLE)
                }
            }.await()

            Response.Success(Response.Action.REMOVE)
        } catch (exception: FirebaseFirestoreException) {
            Response.Error(exception)
        } catch (exception: Exception) {
            Response.Error(exception)
        }
    }

    suspend fun fetchUsingAssetId(assetId: String): Response<Assignment?> = withContext(IO) {
        return@withContext try {
            val task = firestore.collection(Assignment.COLLECTION)
                .whereEqualTo(Assignment.FIELD_ASSET_ID, assetId)
                .orderBy(Assignment.FIELD_ID, Query.Direction.ASCENDING)
                .get().await()

            if (task.isEmpty)
                Response.Success(null)
            else Response.Success(Assignment.from(task.first()))
        } catch(exception: FirebaseFirestoreException) {
            Response.Error(exception)
        } catch(exception: Exception) {
            Response.Error(exception)
        }
    }
}