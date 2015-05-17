package com.androidituts.udp;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.androidplot.ui.SizeLayoutType;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.util.PixelUtils;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.androidplot.Plot;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.Series;
import com.androidplot.xy.*;


public class GaugeAdapter extends BaseAdapter {
	    private Context context;
	    private final ArrayList<Sensor> sensors;

	    private static class ViewHolder {
	        TextView textView;
            TextView valueField;
            TextView minField;
            TextView maxField;
            //TextView timeField;
            //TextView updatesField;
            ProgressBar prog;
            XYPlot plot;
            SimpleXYSeries series1;
	    }
	    
	    public GaugeAdapter(Context mcontext, ArrayList<Sensor> msensors) {
	        context = mcontext;
	        sensors = msensors;
	    }

	     
	    Format domainValueFormat=new Format() {
		     
            // create a simple date format that draws on the year portion of our timestamp.
            // see http://download.oracle.com/javase/1.4.2/docs/api/java/text/SimpleDateFormat.html
            // for a full description of SimpleDateFormat.
            private SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
 
            @Override
            public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
 
                // because our timestamps are in seconds and SimpleDateFormat expects milliseconds
                // we multiply our timestamp by 1000:
                long timestamp = /*System.currentTimeMillis()-*/((Number) obj).longValue()/*-60*60*1000*/;
                Date date = new Date(timestamp);
                return dateFormat.format(date, toAppendTo, pos);
            }
 

			@Override
			public Object parseObject(String arg0, ParsePosition arg1) {
				// TODO Auto-generated method stub
				return null;
			}
        };
	    
	    @Override
	    public View getView(int position, View convertView, ViewGroup parent) {
	        ViewHolder holder = null;
	        final Sensor s=sensors.get(position);
	        boolean forceReCreate=false;

	        if ((convertView != null)) {
	        	//Log.d("UDP", "convview=" + position);
	        	holder = (ViewHolder) convertView.getTag();
	        	/*if ((holder.textView.getText()!=s.id)||
	        		(((TextView)convertView.findViewById(R.id.textView1)).getText()!=s.id))
	        		forceReCreate=true;*/
	        }
	        
	        if ((convertView==null)||forceReCreate) {
	        	Log.d("UDP", "new view=" + position);
	        	LayoutInflater inflater = (LayoutInflater) context
			            .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	            convertView = inflater.inflate(R.layout.item, null);
	            holder = new ViewHolder();
	            holder.textView=(TextView) convertView.findViewById(R.id.textView1);
	            holder.valueField=(TextView) convertView.findViewById(R.id.ValueField);
	            holder.minField=(TextView) convertView.findViewById(R.id.min);
	            holder.maxField=(TextView) convertView.findViewById(R.id.max);
	            //holder.timeField=(TextView) convertView.findViewById(R.id.Time);
	            //holder.updatesField=(TextView) convertView.findViewById(R.id.NumUpdates);
	            //holder.timeField.setVisibility(8);
	            //holder.updatesField.setVisibility(8);
	            
	            holder.prog=(ProgressBar) convertView.findViewById(R.id.ProgressBar1);

	            holder.plot = (XYPlot) convertView.findViewById(R.id.mySimpleXYPlot);

	            holder.plot.getGraphWidget().setSize(new SizeMetrics(
	                    0, SizeLayoutType.FILL,
	                    0, SizeLayoutType.FILL));

	            holder.plot.setTicksPerRangeLabel(3);
	            holder.plot.setTicksPerDomainLabel(2);
	            holder.plot.setDomainStep(XYStepMode.SUBDIVIDE, 13);


            	holder.plot.getGraphWidget().getRangeLabelPaint().setTextSize(PixelUtils.dpToPix(13));
            	holder.plot.getGraphWidget().getRangeOriginLabelPaint().setTextSize(PixelUtils.dpToPix(13));


            	if (!UdpActivity.istablet) { // layout adjustments needed for phones
	            	holder.plot.getGraphWidget().setRangeLabelWidth(85);
	            	holder.plot.getGraphWidget().setRangeLabelVerticalOffset(-10);
	            	holder.plot.getGraphWidget().setDomainLabelWidth(55);
	            	holder.plot.getGraphWidget().setDomainLabelVerticalOffset(-10);
	            	holder.plot.getGraphWidget().setDomainLabelHorizontalOffset(-30);
	            }
            	else holder.plot.getGraphWidget().setDomainLabelHorizontalOffset(-6);

	            
	            holder.plot.setBorderStyle(XYPlot.BorderStyle.NONE, null, null);
	            holder.plot.setPlotMargins(0, 0, 0, 0);
	            holder.plot.setPlotPadding(0, 5, 0, 0);
	            holder.plot.setGridPadding(0, 5, 5, 0);           
	            holder.plot.getLayoutManager().remove(holder.plot.getLegendWidget());            
	            holder.plot.setBackgroundPaint(null);
	            holder.plot.getGraphWidget().setBackgroundPaint(null);
	            holder.plot.getGraphWidget().setGridBackgroundPaint(null);	            
	            //holder.plot.getGraphWidget().setDomainLabelPaint(null);
	            //holder.plot.getGraphWidget().setDomainOriginLabelPaint(null);	            
	            //holder.plot.getLayoutManager().remove(
	            	//	holder.plot.getRangeLabelWidget());
	            //holder.plot.getGraphWidget().setDomainOriginLinePaint(null);
	            //holder.plot.getGraphWidget().setRangeOriginLinePaint(null);
	            //holder.plot.getGraphWidget().setDomainGridLinePaint(null);	            
	            holder.plot.getLayoutManager().remove(holder.plot.getTitleWidget());
	            holder.plot.getLegendWidget().setSize(new SizeMetrics(40, SizeLayoutType.ABSOLUTE, 170, SizeLayoutType.ABSOLUTE));


	            //holder.plot.setRangeValueFormat(new DecimalFormat("0"));	    
	            if (((UdpActivity.numColumns<5)&&(UdpActivity.istablet))||
	            		((UdpActivity.numColumns<2)&&(!UdpActivity.istablet))) {
	            	holder.plot.setDomainValueFormat(domainValueFormat);
	            	holder.plot.getGraphWidget().getDomainLabelPaint().setTextSize(PixelUtils.dpToPix(13));
	            	holder.plot.getGraphWidget().getDomainOriginLabelPaint().setTextSize(PixelUtils.dpToPix(13));
	            }
	            else {
	            	holder.plot.getGraphWidget().setDomainLabelPaint(null);	            	
		            holder.plot.getGraphWidget().setDomainOriginLabelPaint(null);	            
	            }
	            	
/*	            holder.series1 = new SimpleXYSeries(
	                    (s.history),          // SimpleXYSeries takes a List so turn our array into a List
	                    SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, // Y_VALS_ONLY means use the element index as the x value
	                    "Series1");                             // Set the display title of the series*/
	            holder.series1=new SimpleXYSeries("Val");
            	holder.series1.setModel(s.history, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
            	

//	            LineAndPointFormatter f1 =
	           //         new LineAndPointFormatter(Color.rgb(200, 200, 0), null, null, (PointLabelFormatter) null);
	            holder.plot.addSeries(holder.series1, new LineAndPointFormatter(Color.rgb(200, 200, 0), null, null, (PointLabelFormatter) null));

//	        	holder.plot.addSeries(s.history, new LineAndPointFormatter(Color.rgb(200, 200, 0), null, null, (PointLabelFormatter) null));            

	            convertView.setTag(holder);
	        }	            
	        
	        if (position==0) {  // ugly hack to fix why view position 0 never updates on screen
	        	s.changed=true;
	        	s.graphchanged=true;
	        }
            holder.plot.setVisibility(UdpActivity.showgraph?0:8);

	        /*if (!(s.changed||
	            	(s.graphchanged&&UdpActivity.showgraph))/*&&
	            	(s.id==holder.textView.getText())) 
	            		return convertView;*/
	 
            if (s.description==null)
            	holder.textView.setText(s.id);
            else
            	holder.textView.setText(s.description);
            holder.valueField.setText((s.getValue()+"").replace(".0", "")); // ugly hack to remove .0 on integer numbers :)
            holder.prog.setMax((int) ((s.max-s.min)*1000));
            holder.prog.setProgress((int) ((s.getValue()-s.min)*1000));
            holder.minField.setText(s.min+"");
            holder.maxField.setText(s.max+"");
            //holder.maxField.setText(s.timesinceupdated()/1000f+"");
            //holder.updatesField.setText(s.timesupdated+"");
            

            if (UdpActivity.showgraph) {	   
	//            if (s.graphchanged) {
            		//holder.plot.clear();
	            	//holder.series1.addLast(0, s.value);
	            	//holder.series1.addLast(0, s.value);
	            	//holder.series1.notify();
	            	holder.series1.setModel(s.history, SimpleXYSeries.ArrayFormat.XY_VALS_INTERLEAVED);
		            holder.plot.setRangeBoundaries(s.min, s.max, BoundaryMode.FIXED);

	            	//holder.plot.redraw();
	            }

        ///    	s.graphchanged=false;

	       //     }
/*	            else {
	            	holder.plot.setVisibility(8);
		    		//holder.plot.redraw();
	            }*/
	        
	        
	        return convertView;
	    }

	     	    

	    @Override
	    public int getCount() {
	        return sensors.size();
	    }

	    @Override
	    public Object getItem(int position) {
	        return sensors.get(position);
	    }

	    @Override
	    public long getItemId(int position) {
	        return position;
	    }
	    

	    

}

