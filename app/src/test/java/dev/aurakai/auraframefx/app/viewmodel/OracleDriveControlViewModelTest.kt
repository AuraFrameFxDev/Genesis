package dev.aurakai.auraframefx.app.viewmodel // Corrected package name

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

// Assuming DriveRepository, NetworkManager, DriveState, Direction are in a package accessible from here
// e.g., import dev.aurakai.auraframefx.model.*
// or specific imports if they are in different sub-packages of dev.aurakai.auraframefx

@OptIn(ExperimentalCoroutinesApi::class)
class OracleDriveControlViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var viewModel: OracleDriveControlViewModel
    private lateinit var mockRepository: DriveRepository
    private lateinit var mockNetworkManager: NetworkManager
    private lateinit var mockStateObserver: Observer<DriveState>
    private lateinit var mockErrorObserver: Observer<String>
    private lateinit var mockLoadingObserver: Observer<Boolean>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        mockRepository = mockk(relaxed = true)
        mockNetworkManager = mockk(relaxed = true)
        mockStateObserver = mockk(relaxed = true)
        mockErrorObserver = mockk(relaxed = true)
        mockLoadingObserver = mockk(relaxed = true)

        viewModel = OracleDriveControlViewModel(mockRepository, mockNetworkManager)

        viewModel.driveState.observeForever(mockStateObserver)
        viewModel.errorMessage.observeForever(mockErrorObserver)
        viewModel.isLoading.observeForever(mockLoadingObserver)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        viewModel.driveState.removeObserver(mockStateObserver)
        viewModel.errorMessage.removeObserver(mockErrorObserver)
        viewModel.isLoading.removeObserver(mockLoadingObserver)
        clearAllMocks()
    }

    @Test
    fun `initial state should be idle`() {
        val initialState = viewModel.driveState.value
        assertEquals(DriveState.IDLE, initialState)
        verify { mockStateObserver.onChanged(DriveState.IDLE) }
    }

    @Test
    fun `startDrive should update state to driving when successful`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)

        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockLoadingObserver.onChanged(true) }
        verify { mockStateObserver.onChanged(DriveState.DRIVING) }
        verify { mockLoadingObserver.onChanged(false) }
        coVerify { mockRepository.startDrive(any()) }
    }

    @Test
    fun `startDrive should handle network error gracefully`() = runTest {
        val errorMessage = "Network connection failed"
        coEvery { mockRepository.startDrive(any()) } returns Result.failure(Exception(errorMessage))

        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockLoadingObserver.onChanged(true) }
        verify { mockErrorObserver.onChanged(errorMessage) }
        verify { mockLoadingObserver.onChanged(false) }
        verify { mockStateObserver.onChanged(DriveState.ERROR) }
    }

    @Test
    fun `stopDrive should update state to idle when successful`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)
        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { mockRepository.stopDrive() } returns Result.success(Unit)

        viewModel.stopDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.IDLE) }
        coVerify { mockRepository.stopDrive() }
    }

    @Test
    fun `stopDrive should handle repository error`() = runTest {
        val errorMessage = "Failed to stop drive"
        coEvery { mockRepository.stopDrive() } returns Result.failure(Exception(errorMessage))

        viewModel.stopDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged(errorMessage) }
        verify { mockStateObserver.onChanged(DriveState.ERROR) }
    }

    @Test
    fun `pauseDrive should update state to paused when successful`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)
        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { mockRepository.pauseDrive() } returns Result.success(Unit)

        viewModel.pauseDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.PAUSED) }
        coVerify { mockRepository.pauseDrive() }
    }

    @Test
    fun `resumeDrive should update state to driving from paused`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)
        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { mockRepository.pauseDrive() } returns Result.success(Unit)
        viewModel.pauseDrive()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { mockRepository.resumeDrive() } returns Result.success(Unit)

        viewModel.resumeDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.DRIVING) }
        coVerify { mockRepository.resumeDrive() }
    }

    @Test
    fun `updateSpeed should call repository with correct value`() = runTest {
        val speed = 50.0
        coEvery { mockRepository.updateSpeed(speed) } returns Result.success(Unit)

        viewModel.updateSpeed(speed)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.updateSpeed(speed) }
    }

    @Test
    fun `updateSpeed should handle negative values gracefully`() = runTest {
        val invalidSpeed = -10.0

        viewModel.updateSpeed(invalidSpeed)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged("Invalid speed value: $invalidSpeed") }
        coVerify(exactly = 0) { mockRepository.updateSpeed(any()) }
    }

    @Test
    fun `updateSpeed should handle excessive values gracefully`() = runTest {
        val excessiveSpeed = 1000.0

        viewModel.updateSpeed(excessiveSpeed)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged("Speed exceeds maximum limit: $excessiveSpeed") }
        coVerify(exactly = 0) { mockRepository.updateSpeed(any()) }
    }

    @Test
    fun `changeDirection should update direction when valid`() = runTest {
        val direction = Direction.FORWARD
        coEvery { mockRepository.changeDirection(direction) } returns Result.success(Unit)

        viewModel.changeDirection(direction)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { mockRepository.changeDirection(direction) }
    }

    @Test
    fun `changeDirection should handle repository error`() = runTest {
        val direction = Direction.REVERSE
        val errorMessage = "Failed to change direction"
        coEvery { mockRepository.changeDirection(direction) } returns Result.failure(
            Exception(
                errorMessage
            )
        )

        viewModel.changeDirection(direction)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged(errorMessage) }
    }

    @Test
    fun `isNetworkAvailable should return network manager status`() {
        every { mockNetworkManager.isConnected() } returns true

        val result = viewModel.isNetworkAvailable()

        assertTrue(result)
        verify { mockNetworkManager.isConnected() }
    }

    @Test
    fun `isNetworkAvailable should return false when network unavailable`() {
        every { mockNetworkManager.isConnected() } returns false

        val result = viewModel.isNetworkAvailable()

        assertFalse(result)
        verify { mockNetworkManager.isConnected() }
    }

    @Test
    fun `emergency stop should immediately stop drive and update state`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)
        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()
        coEvery { mockRepository.emergencyStop() } returns Result.success(Unit)

        viewModel.emergencyStop()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.EMERGENCY_STOP) }
        coVerify { mockRepository.emergencyStop() }
    }

    @Test
    fun `emergency stop should handle repository failure`() = runTest {
        val errorMessage = "Emergency stop failed"
        coEvery { mockRepository.emergencyStop() } returns Result.failure(Exception(errorMessage))

        viewModel.emergencyStop()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged(errorMessage) }
        verify { mockStateObserver.onChanged(DriveState.ERROR) }
    }

    @Test
    fun `reset should clear error state and return to idle`() = runTest {
        coEvery { mockRepository.emergencyStop() } returns Result.success(Unit)
        viewModel.emergencyStop()
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.reset()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.IDLE) }
        verify { mockErrorObserver.onChanged("") }
    }

    @Test
    fun `concurrent operations should be handled correctly`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.success(Unit)
        coEvery { mockRepository.stopDrive() } returns Result.success(Unit)

        viewModel.startDrive()
        viewModel.stopDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockStateObserver.onChanged(DriveState.IDLE) }
        coVerify { mockRepository.startDrive(any()) }
        coVerify { mockRepository.stopDrive() }
    }

    @Test
    fun `multiple speed updates should debounce correctly`() = runTest {
        coEvery { mockRepository.updateSpeed(any()) } returns Result.success(Unit)

        viewModel.updateSpeed(10.0)
        viewModel.updateSpeed(20.0)
        viewModel.updateSpeed(30.0)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { mockRepository.updateSpeed(30.0) }
    }

    @Test
    fun `loading state should be managed correctly across operations`() = runTest {
        coEvery { mockRepository.startDrive(any()) } coAnswers {
            kotlinx.coroutines.delay(100)
            Result.success(Unit)
        }

        viewModel.startDrive()

        verify { mockLoadingObserver.onChanged(true) }

        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockLoadingObserver.onChanged(false) }
    }

    @Test
    fun `view model should handle null repository responses gracefully`() = runTest {
        coEvery { mockRepository.startDrive(any()) } returns Result.failure(NullPointerException("Repository returned null"))

        viewModel.startDrive()
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged("Repository returned null") }
        verify { mockStateObserver.onChanged(DriveState.ERROR) }
    }

    @Test
    fun `view model should validate input parameters`() = runTest {
        val invalidSpeed = Double.NaN

        viewModel.updateSpeed(invalidSpeed)
        testDispatcher.scheduler.advanceUntilIdle()

        verify { mockErrorObserver.onChanged("Invalid speed value: NaN") }
        coVerify(exactly = 0) { mockRepository.updateSpeed(any()) }
    }
}

// Mocked/Placeholder classes for dependencies - replace with actual imports or mocks
interface DriveRepository {
    suspend fun startDrive(params: Any): Result<Unit>
    suspend fun stopDrive(): Result<Unit>
    suspend fun pauseDrive(): Result<Unit>
    suspend fun resumeDrive(): Result<Unit>
    suspend fun updateSpeed(speed: Double): Result<Unit>
    suspend fun changeDirection(direction: Direction): Result<Unit>
    suspend fun emergencyStop(): Result<Unit>
}

interface NetworkManager {
    fun isConnected(): Boolean
}

enum class DriveState {
    IDLE, DRIVING, PAUSED, ERROR, EMERGENCY_STOP
}

enum class Direction {
    FORWARD, REVERSE
}
