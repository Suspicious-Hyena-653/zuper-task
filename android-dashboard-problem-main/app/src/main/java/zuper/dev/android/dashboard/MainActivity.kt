package zuper.dev.android.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.runBlocking
import zuper.dev.android.dashboard.data.DataRepository
import zuper.dev.android.dashboard.data.model.InvoiceStatus
import zuper.dev.android.dashboard.data.model.JobApiModel
import zuper.dev.android.dashboard.data.model.JobStatus
import zuper.dev.android.dashboard.data.remote.ApiDataSource
import zuper.dev.android.dashboard.ui.theme.AppTheme
import zuper.dev.android.dashboard.ui.theme.BackgroundGrey
import zuper.dev.android.dashboard.ui.theme.BackgroundWhite
import zuper.dev.android.dashboard.ui.theme.BorderGrey
import zuper.dev.android.dashboard.ui.theme.LabelBlue
import zuper.dev.android.dashboard.ui.theme.LabelGreen
import zuper.dev.android.dashboard.ui.theme.LabelPurple
import zuper.dev.android.dashboard.ui.theme.LabelRed
import zuper.dev.android.dashboard.ui.theme.LabelYellow
import zuper.dev.android.dashboard.ui.theme.ShadowGrey
import zuper.dev.android.dashboard.ui.theme.TextBlack
import zuper.dev.android.dashboard.ui.theme.TextGrey
import zuper.dev.android.dashboard.ui.theme.TextSemiBlack
import zuper.dev.android.dashboard.viewmodels.MainViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppTheme {
                // A surface container using the 'background' color from the theme
                val navController = rememberNavController()
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(navController = navController, startDestination = Routes.MainScreen) {
                        composable(Routes.MainScreen) {
                            MainScreen(navigation = navController)
                        }
                        composable(Routes.JobListScreen) {
                            JobListScreen(navigation = navController)
                        }
                    }
                }
            }
        }
    }
}

object Routes {
    const val MainScreen = "MainScreen"
    const val JobListScreen = "JobListScreen"
}

data class Job(
    val jobColor: Color,
    val jobCount: Int
)

data class Invoice(
    val invoiceColor: Color,
    val invoiceValue: Int
)

@Composable
fun MainScreen(mainViewModel: MainViewModel = viewModel(), navigation: NavController) {
    Scaffold(
        topBar = { TitleBar(label = "Dashboard", backButton = false, navigation = navigation) },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .background(BackgroundGrey)
                    .fillMaxSize(),
            ) {
                Divider(
                    thickness = 2.dp,
                    color = ShadowGrey
                )
                NameCard(date = mainViewModel.getSystemDate())
                StatCard(title = "Job Stats", navigation = navigation)
                StatCard(title = "Invoice Stats", navigation = navigation)
            }
        }
    )
}

@Composable
fun JobListScreen(mainViewModel: MainViewModel = viewModel(), navigation: NavController) {
    var jobList = mainViewModel.getJobs()

    var refreshing by remember { mutableStateOf(false) }

    LaunchedEffect(refreshing) {
        if (refreshing) {
            jobList = mainViewModel.getJobs()
            refreshing = false
        }
    }

    var tabIndex by remember { mutableIntStateOf(0) }

    val incompleteJobs =
        Job(LabelRed, jobList.filter { it.status == JobStatus.Incomplete }.size)
    val inProgressJobs =
        Job(LabelBlue, jobList.filter { it.status == JobStatus.InProgress }.size)
    val yetToStartJobs =
        Job(LabelPurple, jobList.filter { it.status == JobStatus.YetToStart }.size)
    val canceledJobs = Job(LabelYellow, jobList.filter { it.status == JobStatus.Canceled }.size)
    val completedJobs =
        Job(LabelGreen, jobList.filter { it.status == JobStatus.Completed }.size)

    val tabs = listOf("Yet to start", "In progress", "Cancelled", "Completed", "Incomplete")

    Scaffold(
        topBar = { TitleBar(label = "Jobs (${jobList.size})", backButton = true, navigation = navigation) },
        content = {
            Column(
                modifier = Modifier
                    .padding(it)
                    .background(BackgroundGrey)
                    .fillMaxSize(),
            ) {
                Divider(
                    thickness = 2.dp,
                    color = ShadowGrey
                )
                Column(
                    modifier = Modifier
                        .background(BackgroundWhite)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "${jobList.size} Jobs",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGrey
                        )
                        Text(
                            text = "${jobList.filter { job -> job.status == JobStatus.Completed }.size} of ${jobList.size} completed",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGrey
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            val jobCountList = arrayListOf(
                                completedJobs,
                                incompleteJobs,
                                inProgressJobs,
                                canceledJobs,
                                yetToStartJobs
                            )

                            jobCountList.sortByDescending { job -> job.jobCount }

                            jobCountList.forEach { job ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(if (job.jobCount.toFloat() == 0.0f) 0.001f else job.jobCount.toFloat())
                                        .background(job.jobColor)
                                )
                            }
                        }
                    }
                }
                Divider(
                    thickness = 1.dp,
                    color = ShadowGrey
                )
                ScrollableTabRow(selectedTabIndex = tabIndex) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            text = { Text(text = title, fontSize = 14.sp, color = TextSemiBlack) },
                            selected = tabIndex == index,
                            onClick = { tabIndex = index }
                        )
                    }
                }
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = refreshing),
                    onRefresh = { refreshing = true },
                ) {
                    LazyColumn(
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        val filteredJobList = when (tabIndex) {
                            0 -> jobList.filter { job -> job.status == JobStatus.YetToStart }
                            1 -> jobList.filter { job -> job.status == JobStatus.InProgress }
                            2 -> jobList.filter { job -> job.status == JobStatus.Canceled }
                            3 -> jobList.filter { job -> job.status == JobStatus.Completed }
                            4 -> jobList.filter { job -> job.status == JobStatus.Incomplete }
                            else -> jobList.filter { job -> job.status == JobStatus.Completed }
                        }

                        items(filteredJobList) { job ->
                            val startDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.ENGLISH).parse(job.startTime)?.time
                                    ?: 0
                            )
                            val endDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.ENGLISH).parse(job.endTime)?.time
                                    ?: 0
                            )
                            val startTime = SimpleDateFormat("hh:mm aa", Locale.ENGLISH).format(
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.ENGLISH).parse(job.startTime)?.time
                                    ?: 0
                            )
                            val endTime = SimpleDateFormat("hh:mm aa", Locale.ENGLISH).format(
                                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss'Z'", Locale.ENGLISH).parse(job.endTime)?.time
                                    ?: 0
                            )

                            Card(
                                border = BorderStroke(1.dp, ShadowGrey),
                                colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "#${job.jobNumber}",
                                        fontSize = 14.sp,
                                        color = TextSemiBlack
                                    )
                                    Text(
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                        text = job.title,
                                        fontSize = 16.sp,
                                        color = TextBlack
                                    )
                                    Text(
                                        text = if (startDate == endDate) "$startDate $startTime - $endTime" else "$startDate $startTime -> $endDate $endTime",
                                        fontSize = 14.sp,
                                        color = TextGrey
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TitleBar(label: String, backButton: Boolean, navigation: NavController) {
    TopAppBar(
        title = {
            Text(
                text = label,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextBlack
            )
        },
        navigationIcon = {
            if (backButton) IconButton(onClick = { navigation.popBackStack() }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    "backIcon"
                )
            }
        },
        colors = topAppBarColors(
            containerColor = BackgroundWhite,
            titleContentColor = TextBlack
        ),
    )
}

@Composable
fun NameCard(date: String) {
    Card(
        border = BorderStroke(1.dp, ShadowGrey),
        colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .background(BackgroundWhite)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Hello, Henry Jones \uD83D\uDC4B",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack
                )
                Text(
                    text = date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGrey,
                    modifier = Modifier
                        .padding(top = 4.dp)
                )
            }
            Box(
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.cat),
                    contentDescription = "Cat",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatCard(mainViewModel: MainViewModel = viewModel(), title: String, navigation: NavController) {
    if (title == "Job Stats") {
        val jobList by mainViewModel.jobStateFlow.collectAsState(emptyList())

        val incompleteJobs =
            Job(LabelRed, jobList.filter { it.status == JobStatus.Incomplete }.size)
        val inProgressJobs =
            Job(LabelBlue, jobList.filter { it.status == JobStatus.InProgress }.size)
        val yetToStartJobs =
            Job(LabelPurple, jobList.filter { it.status == JobStatus.YetToStart }.size)
        val canceledJobs = Job(LabelYellow, jobList.filter { it.status == JobStatus.Canceled }.size)
        val completedJobs =
            Job(LabelGreen, jobList.filter { it.status == JobStatus.Completed }.size)

        Card(
            border = BorderStroke(1.dp, ShadowGrey),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            onClick = { navigation.navigate(Routes.JobListScreen) }
        ) {
            Column(
                modifier = Modifier
                    .background(BackgroundWhite)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Job Stats",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
                Divider(
                    thickness = 2.dp,
                    color = ShadowGrey,
                    modifier = Modifier
                        .padding(top = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "${jobList.size} Jobs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGrey
                    )
                    Text(
                        text = "${jobList.filter { it.status == JobStatus.Completed }.size} of ${jobList.size} completed",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGrey
                    )
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val jobCountList = arrayListOf(
                            completedJobs,
                            incompleteJobs,
                            inProgressJobs,
                            canceledJobs,
                            yetToStartJobs
                        )

                        jobCountList.sortByDescending { it.jobCount }

                        jobCountList.forEach { job ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (job.jobCount.toFloat() == 0.0f) 0.001f else job.jobCount.toFloat())
                                    .background(job.jobColor)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LegendTile(
                        label = "Yet To Start",
                        color = LabelPurple,
                        value = jobList.filter { it.status == JobStatus.YetToStart }.size.toString()
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendTile(
                        label = "In-Progress",
                        color = LabelBlue,
                        value = jobList.filter { it.status == JobStatus.InProgress }.size.toString()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LegendTile(
                        label = "Cancelled",
                        color = LabelYellow,
                        value = jobList.filter { it.status == JobStatus.Canceled }.size.toString()
                    )
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendTile(
                        label = "Completed",
                        color = LabelGreen,
                        value = jobList.filter { it.status == JobStatus.Completed }.size.toString()
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LegendTile(
                        label = "Incomplete",
                        color = LabelRed,
                        value = jobList.filter { it.status == JobStatus.Incomplete }.size.toString()
                    )
                }
            }
        }
    } else {
        val invoiceList by mainViewModel.invoiceStateFlow.collectAsState(emptyList())

        var totalInvoice = 0
        var totalCollected = 0
        var totalDraft = 0
        var totalPending = 0
        var totalBadDebt = 0

        invoiceList.forEach { invoice ->
            totalInvoice += invoice.total

            when (invoice.status) {
                InvoiceStatus.Paid -> totalCollected += invoice.total
                InvoiceStatus.Draft -> totalDraft += invoice.total
                InvoiceStatus.Pending -> totalPending += invoice.total
                InvoiceStatus.BadDebt -> totalBadDebt += invoice.total
            }
        }

        val invoiceCollected = Invoice(LabelGreen, totalCollected)
        val invoicePending = Invoice(LabelBlue, totalPending)
        val invoiceDraft = Invoice(LabelYellow, totalDraft)
        val invoiceBadDebt = Invoice(LabelRed, totalBadDebt)

        Card(
            border = BorderStroke(1.dp, ShadowGrey),
            colors = CardDefaults.cardColors(containerColor = BackgroundWhite),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(BackgroundWhite)
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = "Invoice Stats",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextBlack,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                )
                Divider(
                    thickness = 2.dp,
                    color = ShadowGrey,
                    modifier = Modifier
                        .padding(top = 16.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Total value ($$totalInvoice)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextGrey
                    )
                    Text(
                        text = "$$totalCollected collected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSemiBlack
                    )
                }
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        val invoiceCountList = arrayListOf(
                            invoiceCollected,
                            invoiceDraft,
                            invoicePending,
                            invoiceBadDebt
                        )

                        invoiceCountList.sortByDescending { it.invoiceValue }

                        invoiceCountList.forEach { invoice ->
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(if (invoice.invoiceValue.toFloat() == 0.0f) 0.001f else invoice.invoiceValue.toFloat())
                                    .background(invoice.invoiceColor)
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LegendTile(label = "Draft", color = LabelYellow, value = "$$totalDraft")
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendTile(label = "Pending", color = LabelBlue, value = "$$totalPending")
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    LegendTile(label = "Paid", color = LabelGreen, value = "$$totalCollected")
                    Spacer(modifier = Modifier.width(24.dp))
                    LegendTile(label = "Bad Debt", color = LabelRed, value = "$$totalBadDebt")
                }
            }
        }
    }
}

@Composable
fun LegendTile(label: String, color: Color, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .background(color)
                .size(12.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label ($value)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextGrey
        )
    }
}
