/*
 * Project:  Metro4All
 * Purpose:  Routing in subway.
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * *****************************************************************************
 * Copyright (c) 2015 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.metroaccess.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class FileUtil {
    /**
     * @param fileOrDirectory File to delete.
     * @return Whether the file was deleted.
     */
    public static boolean deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null)
            return false;

        if (fileOrDirectory.isDirectory())
            for (File child : fileOrDirectory.listFiles())
                deleteRecursive(child);

        return fileOrDirectory.delete();
    }

    /**
     * @param filePath File write to.
     * @param sData Data to write.
     * @return Whether the data was written to file.
     */
    public static boolean writeToFile(File filePath, String sData){
        try{
            FileOutputStream os = new FileOutputStream(filePath, false);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(os);
            outputStreamWriter.write(sData);
            outputStreamWriter.close();
            return true;
        }
        catch(IOException e){
            return false;
        }
    }

    /**
     * @param filePath File to read from.
     * @return Data from file.
     */
    public static String readFromFile(File filePath) {
        String ret = "";

        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();

            while ((receiveString = bufferedReader.readLine()) != null) {
                stringBuilder.append(receiveString);
            }

            inputStream.close();
            ret = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return ret;
    }
}
