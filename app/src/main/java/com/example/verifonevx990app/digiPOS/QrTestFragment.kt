package com.example.verifonevx990app.digiPOS

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.verifonevx990app.R
import com.example.verifonevx990app.databinding.FragmentQrTestBinding


class QrTestFragment : Fragment() {
  var binding:FragmentQrTestBinding?=null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentQrTestBinding.inflate(layoutInflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btnShowQr?.setOnClickListener {
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.qrbmp)

            // Set ImageView image as a Bitmap

            // Set ImageView image as a Bitmap
            binding?.qrImg?.setImageBitmap(bitmap)

        }
    }

}