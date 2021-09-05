package io.capstone.ludendorff.features.user

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import io.capstone.ludendorff.api.Backend
import io.capstone.ludendorff.api.request.CreateUserRequest
import io.capstone.ludendorff.api.exception.PreconditionFailedException
import io.capstone.ludendorff.api.exception.UnauthorizedException
import io.capstone.ludendorff.api.request.RemoveUserRequest
import io.capstone.ludendorff.components.persistence.UserProperties
import io.capstone.ludendorff.features.assignment.Assignment
import io.capstone.ludendorff.features.core.backend.Response
import io.capstone.ludendorff.features.department.Department
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userProperties: UserProperties,
    private val backend: Backend
){

    suspend fun create(user: User): Response<Response.Action> {
        return try {
            if (user.email.isNullOrBlank())
                throw NullPointerException()

            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            if (token == null)
                Response.Error(UnauthorizedException(), Response.Action.UPDATE)

            val createUserRequest = CreateUserRequest(
                token = token!!,
                user = user
            )

            val response = backend.newCreateUserPost(createUserRequest)
            when(response.code()) {
                200 -> Response.Success(Response.Action.CREATE)
                401 -> Response.Error(UnauthorizedException(), Response.Action.CREATE)
                412 -> Response.Error(PreconditionFailedException(), Response.Action.CREATE)
                else -> Response.Error(Exception(), Response.Action.CREATE)
            }

        } catch (firestoreException: FirebaseFirestoreException) {
            Response.Error(firestoreException, Response.Action.CREATE)
        } catch (nullPointerException: NullPointerException) {
            Response.Error(nullPointerException, Response.Action.CREATE)
        } catch (exception: Exception) {
            Response.Error(exception, Response.Action.CREATE)
        }
    }

    suspend fun update(user: User): Response<Response.Action> {
        return try {
            val batchWrite = firestore.batch()

            batchWrite.set(firestore.collection(User.COLLECTION)
                .document(user.userId), user)

            if (user.department != null) {
                firestore.collection(Department.COLLECTION)
                    .whereEqualTo(Department.FIELD_MANAGER_ID, user.userId)
                    .get().await()
                    .documents.forEach {
                        val department = it.toObject(Department::class.java)

                        if (department?.departmentId != user.department?.departmentId)
                            batchWrite.update(it.reference, Department.FIELD_MANAGER, null)
                    }
            }

            firestore.collection(Assignment.COLLECTION)
                .whereEqualTo(Assignment.FIELD_USER_ID, user.userId)
                .get().await()
                .documents.forEach {
                    batchWrite.update(it.reference, Assignment.FIELD_USER, user.minimize())
                }

            batchWrite.commit().await()

            firebaseAuth.currentUser?.uid?.let {
                if (it == user.userId) {
                    userProperties.set(user)
                }
            }

            Response.Success(Response.Action.UPDATE)
        } catch (firestoreException: FirebaseFirestoreException) {
            Response.Error(firestoreException, Response.Action.UPDATE)
        } catch (exception: Exception) {
            Response.Error(exception, Response.Action.REMOVE)
        }
    }

    suspend fun update(id: String, fields: Map<String, Any?>): Response<Response.Action> {
        return try {
            firestore.collection(User.COLLECTION)
                .document(id)
                .update(fields)
                .await()

            firebaseAuth.currentUser?.uid?.let {
                if (it == id) {
                    fields.forEach { (field, value) ->
                        if (value is String)
                            userProperties.set(field, value)
                        else if (value is Int)
                            userProperties.set(field, value)
                    }
                }
            }

            Response.Success(Response.Action.UPDATE)
        } catch (firestoreException: FirebaseFirestoreException) {
            Response.Error(firestoreException, Response.Action.UPDATE)
        } catch (exception: Exception) {
            Response.Error(exception, Response.Action.UPDATE)
        }
    }

    suspend fun remove(user: User): Response<Response.Action> {
        return try {
            val token = firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
            if (token == null)
                Response.Error(UnauthorizedException(), Response.Action.UPDATE)

            val removeUserRequest = RemoveUserRequest(
                token = token!!,
                userId = user.userId
            )

            val response = backend.newRemoveUserPost(removeUserRequest)
            when(response.code()) {
                200 -> Response.Success(Response.Action.CREATE)
                401 -> Response.Error(UnauthorizedException(), Response.Action.CREATE)
                412 -> Response.Error(PreconditionFailedException(), Response.Action.CREATE)
                else -> Response.Error(Exception(), Response.Action.CREATE)
            }
        } catch (exception: Exception) {
            Response.Error(exception, Response.Action.REMOVE)
        }
    }
}