package zuper.dev.android.dashboard.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import zuper.dev.android.dashboard.data.DataRepository
import zuper.dev.android.dashboard.data.model.InvoiceApiModel
import zuper.dev.android.dashboard.data.model.JobApiModel
import zuper.dev.android.dashboard.data.remote.ApiDataSource
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel : ViewModel() {
    private val _jobStateFlow = MutableStateFlow<List<JobApiModel>>(emptyList())
    val jobStateFlow: StateFlow<List<JobApiModel>> = _jobStateFlow

    private val _invoiceStateFlow = MutableStateFlow<List<InvoiceApiModel>>(emptyList())
    val invoiceStateFlow: StateFlow<List<InvoiceApiModel>> = _invoiceStateFlow

    private val dataRepository = DataRepository(ApiDataSource())

    init {
        viewModelScope.launch {
            dataRepository.observeJobs().collect { jobList -> _jobStateFlow.value = jobList }
        }

        viewModelScope.launch {
            dataRepository.observeInvoices().collect { invoiceList -> _invoiceStateFlow.value = invoiceList }
        }
    }

    fun getJobs(): List<JobApiModel> {
        return dataRepository.getJobs()
    }

    fun getSystemDate(): String {
        val currentDate = Date()

        val dayFormat = SimpleDateFormat("EEEE", Locale.ENGLISH)
        val monthFormat = SimpleDateFormat("MMMM", Locale.ENGLISH)
        val dateFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val yearFormat = SimpleDateFormat("yyyy", Locale.ENGLISH)

        val dateInInt = dateFormat.format(currentDate).toInt()
        var dateWithSuffix = ""

        dateWithSuffix = if (dateInInt in 11..13) {
            "${dateInInt}th"
        } else {
            when (dateInInt % 10) {
                1 -> "${dateInInt}st"
                2 -> "${dateInInt}nd"
                3 -> "${dateInInt}rd"
                else -> "${dateInInt}th"
            }
        }

        return "${dayFormat.format(currentDate)}, ${monthFormat.format(currentDate)} $dateWithSuffix ${
            yearFormat.format(
                currentDate
            )
        }"
    }
}