package zuper.dev.android.dashboard.data.remote

import zuper.dev.android.dashboard.data.model.InvoiceApiModel
import zuper.dev.android.dashboard.data.model.InvoiceStatus
import zuper.dev.android.dashboard.data.model.JobApiModel
import zuper.dev.android.dashboard.data.model.JobStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.random.Random

object SampleData {

    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")

    fun generateRandomJobList(size: Int): List<JobApiModel> {
        val random = Random
        return (1..size).map {
            JobApiModel(
                jobNumber = it,
                title = generateRandomJobTitle(),
                startTime = LocalDateTime.now().plusDays(random.nextLong(1, 30))
                    .format(isoFormatter),
                endTime = LocalDateTime.now().plusDays(random.nextLong(31, 60))
                    .format(isoFormatter),
                status = when (random.nextInt(5)) {
                    0 -> JobStatus.YetToStart
                    1 -> JobStatus.InProgress
                    2 -> JobStatus.Canceled
                    3 -> JobStatus.Completed
                    else -> JobStatus.Incomplete
                }
            )
        }
    }

    fun generateRandomInvoiceList(size: Int): List<InvoiceApiModel> {
        val random = Random
        return (1..size).map {
            InvoiceApiModel(
                invoiceNumber = random.nextInt(1, Int.MAX_VALUE),
                customerName = generateRandomCustomerName(),
                total = random.nextInt(1, 10) * 1000,
                status = when (random.nextInt(4)) {
                    0 -> InvoiceStatus.Draft
                    1 -> InvoiceStatus.Pending
                    2 -> InvoiceStatus.Paid
                    else -> InvoiceStatus.BadDebt
                }
            )
        }
    }
}

private fun generateRandomJobTitle(): String {
    val adjectives = listOf("Amazing", "Fantastic", "Awesome", "Incredible", "Superb")
    val nouns = listOf("Job", "Task", "Project", "Assignment", "Work")

    val randomAdjective = adjectives.random()
    val randomNoun = nouns.random()

    return "$randomAdjective $randomNoun"
}

fun generateRandomCustomerName(): String {
    val firstNames = listOf("John", "Jane", "Alice", "Bob", "Eva")
    val lastNames = listOf("Doe", "Smith", "Johnson", "Brown", "Lee")

    val randomFirstName = firstNames.random()
    val randomLastName = lastNames.random()

    return "$randomFirstName $randomLastName"
}
