package com.hltc.tennis

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hltc.tennis.data.MatchMode
import com.hltc.tennis.data.toLongList
import com.hltc.tennis.ui.theme.HLTCTennisTheme
import com.hltc.tennis.viewmodel.MainViewModel
import com.hltc.tennis.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = AppContainer(applicationContext)
        setContent {
            HLTCTennisTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(container.repository))
                AppRoot(vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("홈", "플레이어", "세션", "통계")
    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(Modifier.padding(padding).padding(12.dp)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, t -> Tab(selected = tab == i, onClick = { tab = i }, text = { Text(t) }) }
            }
            Spacer(Modifier.height(12.dp))
            when (tab) {
                0 -> HomeScreen(state.players.size, state.sessions.size) { vm.seedDemo() }
                1 -> PlayerScreen(vm)
                2 -> SessionScreen(vm)
                3 -> StatsScreen(vm)
            }
        }
    }
}

@Composable
private fun HomeScreen(playerCount: Int, sessionCount: Int, onDemo: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = onDemo, modifier = Modifier.fillMaxWidth()) { Text("데모 데이터 넣기") }
        Card(Modifier.fillMaxWidth()) { Text("등록 플레이어: $playerCount", Modifier.padding(12.dp)) }
        Card(Modifier.fillMaxWidth()) { Text("누적 세션: $sessionCount", Modifier.padding(12.dp)) }
    }
}

@Composable
private fun PlayerScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    var cohort by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var level by remember { mutableIntStateOf(3) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = cohort, onValueChange = { cohort = it }, label = { Text("기수") }, modifier = Modifier.weight(1f))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("이름") }, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("레벨: $level")
            Button(onClick = { if (level > 1) level-- }) { Text("-") }
            Button(onClick = { if (level < 5) level++ }) { Text("+") }
            Button(onClick = { if (cohort.isNotBlank() && name.isNotBlank()) vm.addPlayer(cohort, name, level, "") }) { Text("추가") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.players) { p -> Card(Modifier.fillMaxWidth()) { Text("${p.cohort} ${p.name} (Lv${p.level})", Modifier.padding(10.dp)) } }
        }
    }
}

@Composable
private fun SessionScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val participants = remember { mutableStateListOf<Long>() }
    var games by remember { mutableStateOf("6") }
    var mode by remember { mutableStateOf(MatchMode.RANDOM) }
    val latest = state.sessions.firstOrNull()
    val matches by (if (latest != null) vm.observeMatches(latest.id) else kotlinx.coroutines.flow.flowOf(emptyList())).collectAsState(initial = emptyList())

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("세션 생성", style = MaterialTheme.typography.titleMedium)
        state.players.filter { it.active }.forEach { p ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(checked = participants.contains(p.id), onCheckedChange = { if (it) participants.add(p.id) else participants.remove(p.id) })
                Text("${p.name} (Lv${p.level})")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = games, onValueChange = { games = it.filter(Char::isDigit) }, label = { Text("총 게임 수") })
            FilterChip(selected = mode == MatchMode.RANDOM, onClick = { mode = MatchMode.RANDOM }, label = { Text("랜덤") })
            FilterChip(selected = mode == MatchMode.BALANCED, onClick = { mode = MatchMode.BALANCED }, label = { Text("밸런스") })
        }
        Button(onClick = {
            val g = games.toIntOrNull() ?: 0
            if (participants.size >= 4 && g > 0) vm.createSession("오늘", participants.toList(), g, mode)
        }) { Text("오늘 세션 시작") }

        Text("오늘 경기 리스트", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(matches.sortedBy { it.orderIndex }) { m ->
                val status = if (m.status == "COMPLETED") "완료" else "예정"
                var a by remember { mutableStateOf(m.scoreA?.toString() ?: "") }
                var b by remember { mutableStateOf(m.scoreB?.toString() ?: "") }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("#${m.orderIndex + 1} [$status] A:${m.teamAPlayerIds} vs B:${m.teamBPlayerIds}")
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(value = a, onValueChange = { a = it.filter(Char::isDigit) }, label = { Text("A") })
                            OutlinedTextField(value = b, onValueChange = { b = it.filter(Char::isDigit) }, label = { Text("B") })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(onClick = { vm.saveResult(m, a.toIntOrNull() ?: 0, b.toIntOrNull() ?: 0) }) { Text("결과 저장") }
                            Button(onClick = { vm.resetResult(m) }) { Text("결과 초기화") }
                            Button(onClick = {
                                val ids = (m.teamAPlayerIds.toLongList() + m.teamBPlayerIds.toLongList())
                                if (ids.size == 4) vm.updateLineup(m, listOf(ids[0], ids[2]), listOf(ids[1], ids[3]))
                            }) { Text("팀 스왑") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsScreen(vm: MainViewModel) {
    val state by vm.uiState.collectAsState()
    val byId = state.players.associateBy { it.id }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(state.stats.values.sortedByDescending { it.winRate }) { s ->
            val p = byId[s.playerId]
            Card(Modifier.fillMaxWidth()) {
                Text("${p?.name ?: s.playerId} 경기 ${s.matches}승 ${s.wins}패 ${s.losses} 승률 ${(s.winRate * 100).toInt()}%", Modifier.padding(10.dp))
            }
        }
    }
}
