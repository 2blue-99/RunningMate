package com.running.runningmate2.viewModel.fragmentViewModel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.running.data.local.sharedPreference.SharedPreferenceHelperImpl
import com.running.domain.SavedData.DomainWeather
import com.running.domain.state.ResourceState
import com.running.domain.usecase.GetWeatherUseCase
import com.running.domain.usecase.LocationUseCase
import com.running.runningmate2.base.BaseViewModel
import com.running.runningmate2.utils.Calorie
import com.running.runningmate2.utils.MyApplication
import com.running.runningmate2.utils.ListLiveData
import com.running.runningmate2.utils.MapState
import com.running.runningmate2.utils.TimeHelper
import com.running.runningmate2.utils.WeatherHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapsViewModel @Inject constructor(
    private val getWeatherUseCase: GetWeatherUseCase,
    private val sharedPreferences: SharedPreferenceHelperImpl,
        private val locationUseCase: LocationUseCase,
        private val sharedPreferenceHelperImpl: SharedPreferenceHelperImpl
) : BaseViewModel(), SensorEventListener {

    private val _distance = MutableLiveData<Double>()
    val distance: LiveData<Double> get() = _distance

    private val _calorie = MutableLiveData<Double>()
    val calorie: LiveData<Double> get() = _calorie

    private val _time = MutableLiveData<String>()
    val time: LiveData<String> get() = _time

    private val _step = MutableLiveData(0)
    val step: LiveData<Int> get() = _step

    private val _weatherData = MutableLiveData<ResourceState<DomainWeather>>()
    val weatherData: LiveData<ResourceState<DomainWeather>> get() = _weatherData
    fun isWeatherLoading() = _weatherData.value is ResourceState.Loading

    private var _second = 0
    private var _minute = 0
    private var _hour = 0
    private var second = ""
    private var minute = ""
    private var hour = ""
    private var accel: Float = 0.0f
    private var accelCurrent: Float = 0.0f
    private var accelLast: Float = 0.0f
    lateinit var sensorManager: SensorManager
    private var skip = 300L
    private var myShakeTime = 0L

    private val _mapState = MutableLiveData<MapState>()
    val mapState: LiveData<MapState> get() = _mapState
    fun changeState(state: MapState){ _mapState.value = state }

    private val _location = ListLiveData<Location>()
    val location: LiveData<ArrayList<Location>> get() = _location
    fun getNowLocation(): Location? = _location.value?.last()
    val loading: LiveData<Boolean> get() = isLoading

    private var locationStream: Job? = null
    fun startSteam(){
        locationStream = locationUseCase.getLocationDataStream().onEach {
            when (it) {
                is ResourceState.Success -> {
                    isLoading.value = false
                    _location.add(it.data)
                }
                else -> {
                    isLoading.value = true
                    _location.add(Location(null))
                }
            }
            Log.e("TAG", "${_location.value}: ", )
        }.launchIn(modelScope)
    }

    fun getWeatherData() {
        modelScope.launch {
            WeatherHelper.makeRequest(_location.value?.last()).let { request ->
                getWeatherUseCase(request).onEach {
                    when(it){
                        is ResourceState.Success -> {
                            _weatherData.postValue(it)
                        }
                        else -> {
                            _weatherData.postValue(it)
                        }
                    }
                }.catch {
                    _weatherData.postValue(ResourceState.Error(message = "UnHandle Err"))
                }.launchIn(modelScope)
            }
        }
    }
    fun startTime() {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                _time.value = TimeHelper().getCustomTime()
            }
        }
    }

    fun calculateDistance() {
        if(_location.size() > 1){
            var result = _location.getFirst().distanceTo(_location.getSecond()).toDouble()
            if (result <= 2) result = 0.0
            _distance.value = _distance.value?.plus(result)
            if (result != 0.0) calculateCalorie()
        }
    }

    private fun calculateCalorie(){
        val weight = sharedPreferences.getWeight()
        _calorie.value = _calorie.value?.plus(Calorie(weight).myCalorie())
    }

    @SuppressLint("ServiceCast")
    fun senSor(application: Application) {
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accel = 10f
        accelCurrent = SensorManager.GRAVITY_EARTH
        accelLast = SensorManager.GRAVITY_EARTH

        sensorManager.registerListener(
            this, sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME
        )

    }
    override fun onSensorChanged(event: SensorEvent?) {
        val x: Float = event?.values?.get(0) as Float
        val y: Float = event.values?.get(1) as Float
        val z: Float = event.values?.get(2) as Float

        accelLast = accelCurrent
        accelCurrent = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        accel = accel * 0.9f + accelCurrent - accelLast

        if (accel > 15) {
            val currentTime = System.currentTimeMillis()
            if (myShakeTime + skip > currentTime) return
            myShakeTime = currentTime
            _step.value = _step.value?.plus(1)
        }
    }
    fun startStep() { senSor(MyApplication.getApplication()) }
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    private fun killSensor() { sensorManager.unregisterListener(this) }
    fun saveWeight(weight: String) = sharedPreferenceHelperImpl.saveWeight(weight.toInt())
    fun getWeight(): Int = sharedPreferenceHelperImpl.getWeight()
    fun stepInit() { killSensor() }
    fun removeSteam() { locationStream?.cancel() }
}
