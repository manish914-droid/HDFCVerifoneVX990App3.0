package com.example.verifonevx990app.init

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.PagerAdapter
import com.example.verifonevx990app.R
import com.example.verifonevx990app.appupdate.SendAppUpdateConfirmationPacket
import com.example.verifonevx990app.appupdate.SyncAppUpdateConfirmation
import com.example.verifonevx990app.databinding.FragmentDashboardBinding
import com.example.verifonevx990app.disputetransaction.CreateSettlementPacket
import com.example.verifonevx990app.main.IFragmentRequest
import com.example.verifonevx990app.main.MainActivity
import com.example.verifonevx990app.main.SETTLEMENT
import com.example.verifonevx990app.realmtables.BHDashboardItem
import com.example.verifonevx990app.realmtables.BatchFileDataTable
import com.example.verifonevx990app.realmtables.EDashboardItem
import com.example.verifonevx990app.realmtables.TerminalParameterTable
import com.example.verifonevx990app.vxUtils.*
import kotlinx.coroutines.*
import java.lang.Runnable
import java.util.*


class DashboardFragment : Fragment() {

    companion object {
        var toRefresh = true
    }

    private var iFragmentRequest: IFragmentRequest? = null
    private val itemList = mutableListOf<EDashboardItem>()
    private val list1 = arrayListOf<EDashboardItem>()
    private val list2 = arrayListOf<EDashboardItem>()

    /* private val mAdapter by lazy {
         DashBoardAdapter(iFragmentRequest, ::onItemLessMoreClick)
     }*/

    private val dashBoardAdapter by lazy {
        DashBoardAdapter(
            iFragmentRequest,
            ::onItemLessMoreClick
        )
    }
    private var animShow: Animation? = null
    private var animHide: Animation? = null

    private val imageArr: IntArray by lazy {
        intArrayOf(
            R.drawable.banner_3,
            R.drawable.banner_1,
            R.drawable.banner_2,
            R.drawable.banner_4
        )
    }
    private val imageAdapter by lazy { activity?.let { ImagePagerAdapterEMI(it, imageArr) } }
    private var counter = 0
    private var isUpdate = false
    private var binding: FragmentDashboardBinding? = null


    //for image  scrolling in viewpager
    private var currentPage = 0

    //timer for image scrolling in viewpager
    private var timer: Timer? = null
    private val DELAY_MS: Long = 500 //delay in milliseconds before task is to be executed
    private val PERIOD_MS: Long = 3000
    private val handler = Handler(Looper.getMainLooper())
    private var runnable: Runnable? = null
    private var tptData: TerminalParameterTable? = null
    private var batchData: MutableList<BatchFileDataTable> = mutableListOf()

    // For view pager scrolling
    var update: Runnable? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isDashboardOpen = true

        if (isExpanded) {
            binding?.pagerViewLL?.visibility = View.GONE
        } else {
            binding?.pagerViewLL?.visibility = View.VISIBLE
        }

        update = Runnable {
            if (currentPage == imageArr.size) {
                currentPage = 0
            }
            if (binding?.photosViewpager != null)
                binding?.photosViewpager?.setCurrentItem(currentPage++, true)
        }

        GlobalScope.launch(Dispatchers.IO) {
            initAnimation()
            withContext(Dispatchers.Main) {
                binding?.pagerViewLL?.startAnimation(animShow)
                setUpImageViewPager()
            }
            setUpDashBoardItems()
        }

        (activity as MainActivity).showHelpDeskNumber()
        Log.d("Current Time:- ", getTimeInMillis().toString())

        //region======================Change isAutoSettleDone Boolean Value to False if Date is greater then
        //last saved Auto Settle Date:-
        if (!TextUtils.isEmpty(AppPreference.getString(AppPreference.LAST_SAVED_AUTO_SETTLE_DATE))) {
            if (AppPreference.getString(AppPreference.LAST_SAVED_AUTO_SETTLE_DATE).toInt() > 0) {
                if (getSystemTimeIn24Hour().terminalDate().toInt() >
                    AppPreference.getString(AppPreference.LAST_SAVED_AUTO_SETTLE_DATE).toInt()
                ) {
                    AppPreference.saveBoolean(AppPreference.IsAutoSettleDone, false)
                }
            }
        }
        //endregion

        //region=======================Check For AutoSettle at regular interval if App is on Dashboard:-
        if (isDashboardOpen && !AppPreference.getBoolean(AppPreference.IsAutoSettleDone))
            checkForAutoSettle()
        //endregion
    }

    //region============================Auto Settle Check on Dashboard at Regular Intervals:-
    private fun checkForAutoSettle() {
        runnable = object : Runnable {
            override fun run() {
                try {
                    Log.d("AutoSettle:- ", "Checking....")
                    autoSettleBatch()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    //also call the same runnable to call it at regular interval
                    handler.postDelayed(this, 20000)
                }
            }
        }
        handler.post(runnable as Runnable)
    }
    //endregion

    //region=======================Check for User IDLE on Dashboard and do auto settle if conditions match:-
    private fun autoSettleBatch() {
        val tptData = runBlocking(Dispatchers.IO) { TerminalParameterTable.selectFromSchemeTable() }
        val batchData = runBlocking(Dispatchers.IO) { BatchFileDataTable.selectBatchData() }
        Log.d("HostForceSettle:- ", tptData?.forceSettle ?: "")
        Log.d("HostForceSettleTime:- ", tptData?.forceSettleTime ?: "")
        Log.d(
            "System Date Time:- ",
            "${getSystemTimeIn24Hour().terminalDate()} ${getSystemTimeIn24Hour().terminalTime()}"
        )
        if (isDashboardOpen && !AppPreference.getBoolean(AppPreference.IsAutoSettleDone)) {
            Log.d("Dashboard Open:- ", "Yes")
            if (!TextUtils.isEmpty(tptData?.forceSettle)
                && !TextUtils.isEmpty(tptData?.forceSettleTime)
                && tptData?.forceSettle == "1"
            ) {
                if ((tptData.forceSettleTime == getSystemTimeIn24Hour().terminalTime()
                            || getSystemTimeIn24Hour().terminalTime() > tptData.forceSettleTime
                            ) && batchData.size > 0
                ) {
                    logger("Auto Settle:- ", "Auto Settle Available")
                    val data = runBlocking(Dispatchers.IO) {
                        CreateSettlementPacket(
                            ProcessingCode.SETTLEMENT.code, batchData
                        ).createSettlementISOPacket()
                    }
                    GlobalScope.launch(Dispatchers.IO) {
                        val settlementByteArray = data.generateIsoByteRequest()
                        (activity as MainActivity).settleBatch(
                            settlementByteArray,
                            SETTLEMENT.DASHBOARD.type
                        )
                    }
                } else
                    logger("Auto Settle:- ", "Auto Settle Mismatch Time")
            } else
                logger("Auto Settle:- ", "Auto Settle Not Available")
        } else {
            Log.d("Dashboard Close:- ", "Yes")
        }
    }
    //endregion


    private fun scheduleTimer() {
        timer = Timer()
        timer?.schedule(object : TimerTask() {
            // task to be scheduled
            override fun run() {
                update?.let { Handler(Looper.getMainLooper()).post(it) }
            }
        }, DELAY_MS, PERIOD_MS)
    }

    override fun onResume() {
        super.onResume()
        scheduleTimer()
    }

    override fun onPause() {
        super.onPause()
        timer?.cancel()
        runnable?.let { handler.removeCallbacks(it) }
    }

    private suspend fun setUpImageViewPager() {
        binding?.photosViewpager?.adapter = imageAdapter
        binding?.tabLL?.setupWithViewPager(binding?.photosViewpager)
    }

    private suspend fun initAnimation() {
        animShow = AnimationUtils.loadAnimation(activity, R.anim.view_show)
        animHide = AnimationUtils.loadAnimation(activity, R.anim.view_hide)
    }

    private fun onItemLessMoreClick(item: EDashboardItem) {
        when (item) {
            EDashboardItem.MORE -> {
                isExpanded = true
                binding?.pagerViewLL?.startAnimation(animHide)
                binding?.pagerViewLL?.visibility = View.GONE
                dashBoardAdapter.onUpdatedItem(list2)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            EDashboardItem.LESS -> {
                isExpanded = false
                binding?.pagerViewLL?.visibility = View.VISIBLE
                binding?.pagerViewLL?.startAnimation(animShow)
                dashBoardAdapter.onUpdatedItem(list1)
                dashBoardAdapter.notifyDataSetChanged()
                binding?.dashboardRV?.scheduleLayoutAnimation()
            }
            else -> {
            }
        }
    }

    // region ======SetUp DashBoard items
    private suspend fun setUpDashBoardItems() {
        //Fetching Update Key Value From AppPreference Memory to Default Send Confirmation Packet for First Time After App Update
        //if Merchant using same AppVersion but getting App Update
        isUpdate = AppPreference.getBoolean("isUpdate")
        Log.d("Update Value:- ", isUpdate.toString())

        //Below method is only called once after App is Updated to newer version:-
        //sendConfirmationToHost()

        if (toRefresh || itemList.isEmpty()) {
            itemList.clear()
            list1.clear()
            list2.clear()
            val tpt = TerminalParameterTable.selectFromSchemeTable()

            if (tpt != null) {
                val tableClass =
                    tpt::class.java //Class Name (class com.bonushub.pax.utilss.TerminalParameterTable)
                for (e in tableClass.declaredFields) {
                    val ann = e.getAnnotation(BHDashboardItem::class.java)
                    //If table's field  having the particular annotation as @BHDasboardItem then it returns the value ,If not then return null
                    if (ann != null) {
                        e.isAccessible = true
                        val t = e.get(tpt) as String
                        if (t == "1") {
                            itemList.add(ann.item)
                            if (ann.childItem != EDashboardItem.NONE) {
                                itemList.add(ann.childItem)
                            }
                        }
                    }
                }
                //Adding Field HDFC Cross Sell in Dashboard List:-
                //  tpt.reservedValues = "00000000000000010000"
                when {
                    tpt.reservedValues[13].toString().toInt() == 1 ||
                            tpt.reservedValues[14].toString().toInt() == 1 ||
                            tpt.reservedValues[15].toString().toInt() == 1 ||
                            tpt.reservedValues[16].toString().toInt() == 1 -> itemList.add(
                        EDashboardItem.CROSS_SELL
                    )
                }

            } else {
                itemList.add(EDashboardItem.NONE)
            }
            // This list is a list where all types of preath available which was enable by backend
            val totalPreAuthItem = mutableListOf<EDashboardItem>()
            totalPreAuthItem.addAll(itemList)

            //After converting we are getting the total preauth trans type available(by retainAll fun)
            //It returns true if any praauth item is available and return false if no preauth item found
            val isAnyPreAuthItemAvailable = totalPreAuthItem.retainAll { item ->
                item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                        || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
            }

            if (isAnyPreAuthItemAvailable) {
                itemList.removeAll { item ->
                    item == EDashboardItem.PREAUTH || item == EDashboardItem.PREAUTH_COMPLETE
                            || item == EDashboardItem.VOID_PREAUTH || item == EDashboardItem.PENDING_PREAUTH
                }
                val preAuth = EDashboardItem.PRE_AUTH_CATAGORY
                preAuth.childList = totalPreAuthItem
                itemList.add(preAuth)
            }

            itemList.sortWith(compareBy { it.rank })
            // Below code is used for dashboard items divided into view less and view more functionality
            for (lst in itemList.indices) {
                if (lst <= 5) {
                    list1.add(itemList[lst])
                } else {
                    list1[5] = EDashboardItem.MORE
                    list2.addAll(itemList)
                    list2.add(EDashboardItem.LESS)
                    break
                }
            }

            // Setting up recyclerview of DashBoard Items
            withContext(Dispatchers.Main) {
                binding?.dashboardRV?.apply {
                    layoutManager = GridLayoutManager(activity, 3)
                    itemAnimator = DefaultItemAnimator()
                    adapter = dashBoardAdapter
                    if (isExpanded) dashBoardAdapter.onUpdatedItem(list2) else dashBoardAdapter.onUpdatedItem(
                        list1
                    )
                    scheduleLayoutAnimation()
                }

            }

        } else {
            binding?.dashboardRV?.apply {
                layoutManager = GridLayoutManager(activity, 3)
                itemAnimator = DefaultItemAnimator()
                adapter = dashBoardAdapter
                if (isExpanded) dashBoardAdapter.onUpdatedItem(list2) else dashBoardAdapter.onUpdatedItem(
                    list1
                )
                scheduleLayoutAnimation()
            }
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is IFragmentRequest) {
            iFragmentRequest = context
        }
        if (context is IDialog) {
            context.onEvents(VxEvent.ChangeTitle(getString(R.string.app_name)))
        }
    }

    override fun onDetach() {
        super.onDetach()
        iFragmentRequest = null
        binding = null
    }

    /*Below method only executed when the app is updated to newer version and
    previous store version in file < new app updated version:- */
    private fun sendConfirmationToHost() {
        try {
            context?.let {
                getRevisionIDFromFile(it) { isRevisionIDSame ->
                    if (isRevisionIDSame || !isUpdate) {
                        sendConfirmation()
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
            VFService.showToast(getString(R.string.confirmation_app_update_failed))
        }
    }

    //Sync App Confirmation to Host:-
    private fun sendConfirmation() {
        val appUpdateConfirmationISOData =
            SendAppUpdateConfirmationPacket().createAppUpdateConfirmationPacket()
        val isoByteArray = appUpdateConfirmationISOData.generateIsoByteRequest()
        GlobalScope.launch(Dispatchers.Main) {
            (activity as MainActivity).showProgress(getString(R.string.please_wait))
        }
        SyncAppUpdateConfirmation(isoByteArray) { syncStatus ->
            GlobalScope.launch(Dispatchers.Main) {
                AppPreference.saveBoolean("isUpdate", true)
                (activity as MainActivity).hideProgress()
                if (syncStatus) {
                    context?.let { it1 ->
                        writeAppRevisionIDInFile(it1)
                    }
                } else {
                    counter += 1
                    if (counter < 2) {
                        sendConfirmation()
                    }
                }
            }
        }
    }
}

class DashBoardAdapter(
    private val fragReq: IFragmentRequest?,
    var lessMoreClick: (item: EDashboardItem) -> Unit
) : RecyclerView.Adapter<DashBoardAdapter.DashBoardViewHolder>() {
    var mList: ArrayList<EDashboardItem> = arrayListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DashBoardViewHolder {
        return DashBoardViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_dashboart, parent, false)
        )
    }

    override fun getItemCount(): Int = mList.size

    override fun onBindViewHolder(holder: DashBoardViewHolder, position: Int) {
        holder.logoIV.background =
            ContextCompat.getDrawable(holder.view.context, mList[position].res)
        holder.titleTV.text = mList[position].title
        holder.itemParent.setOnClickListener {
            if (mList[position] == EDashboardItem.LESS || mList[position] == EDashboardItem.MORE)
                lessMoreClick(mList[position])
            else
                fragReq?.onDashBoardItemClick(mList[position])
        }

    }

    fun onUpdatedItem(list: List<EDashboardItem>) {
        mList.clear()
        mList.addAll(list)

    }


    inner class DashBoardViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val logoIV: ImageView = view.findViewById(R.id.item_logo_iv)
        val titleTV: TextView = view.findViewById(R.id.item_title_tv)
        val itemParent: ConstraintLayout = view.findViewById(R.id.item_parent_rv)

    }
}

class ImagePagerAdapterEMI(val context: Context, private val imgArr: IntArray) : PagerAdapter() {

    private val layoutInflater: LayoutInflater by lazy {
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    }

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj as CardView
    }

    override fun getCount(): Int = imgArr.size

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val itemView: View = layoutInflater.inflate(R.layout.item_adapter_img, container, false)
        val imageView = itemView.findViewById<View>(R.id.pagerImageView) as ImageView
        imageView.setImageResource(imgArr[position])
        container.addView(itemView)
        //listening to image click
        imageView.setOnClickListener {

            Log.i("VIEW PAGER", "you clicked image " + (position + 1))
        }
        return itemView
    }

    override fun destroyItem(container: ViewGroup, position: Int, obj: Any) {
        container.removeView(obj as CardView)
    }
}


